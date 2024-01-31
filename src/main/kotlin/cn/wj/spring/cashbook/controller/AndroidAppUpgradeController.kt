package cn.wj.spring.cashbook.controller

import cn.wj.spring.cashbook.entity.CbResponse
import cn.wj.spring.cashbook.entity.CbResponse.Companion.RESPONSE_CODE_FAILED_NO_RES
import cn.wj.spring.cashbook.entity.DriveEntity
import cn.wj.spring.cashbook.entity.GitReleaseEntity
import cn.wj.spring.cashbook.entity.MicrosoftTokenEntity
import cn.wj.spring.cashbook.entity.ReleaseEntity
import cn.wj.spring.cashbook.enums.SourceEnum
import com.alicp.jetcache.Cache
import com.alicp.jetcache.CacheManager
import com.alicp.jetcache.template.QuickConfig
import org.apache.hc.client5.http.impl.DefaultRedirectStrategy
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder
import org.apache.hc.core5.http.HttpRequest
import org.apache.hc.core5.http.HttpResponse
import org.apache.hc.core5.http.protocol.HttpContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.client.ClientHttpResponse
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.DefaultResponseErrorHandler
import org.springframework.web.client.RestTemplate
import java.util.concurrent.TimeUnit

/**
 * Android 应用升级相关
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2024/1/30
 */
@RestController
class AndroidAppUpgradeController {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    @Autowired
    private lateinit var cacheManager: CacheManager

    private val tokenCache: Cache<String, String> by lazy {
        cacheManager.getOrCreateCache(QuickConfig.newBuilder("microsoft_token").build())
    }

    private val configCache: Cache<String, String> by lazy {
        cacheManager.getOrCreateCache(QuickConfig.newBuilder("config").build())
    }

    private val restTemplate: RestTemplate by lazy {
        RestTemplate().apply {
            setRequestFactory(HttpComponentsClientHttpRequestFactory())
            errorHandler = object : DefaultResponseErrorHandler() {
                override fun hasError(response: ClientHttpResponse): Boolean {
                    return false
                }
            }
        }
    }

    private val noRedirectRestTemplate: RestTemplate by lazy {
        RestTemplate().apply {
            setRequestFactory(HttpComponentsClientHttpRequestFactory(
                    HttpClientBuilder.create().useSystemProperties().setRedirectStrategy(object : DefaultRedirectStrategy() {
                        override fun isRedirected(request: HttpRequest?, response: HttpResponse?, context: HttpContext?): Boolean {
                            return false
                        }
                    }).build()
            ))
            errorHandler = object : DefaultResponseErrorHandler() {
                override fun hasError(response: ClientHttpResponse): Boolean {
                    return false
                }
            }
        }
    }

    @GetMapping("v1/cb/android/upgrade/latest")
    fun latest(
            @RequestParam(name = "source", defaultValue = "0") source: String,
            @RequestParam(name = "canary", defaultValue = "0") canary: String
    ): CbResponse<*> {
        val sourceEnum = SourceEnum.fromValue(source)
        val canaryEnable = "1" == canary
        logger.info("latest(source = <$source>, canary = <$canary>)")
        return runCatching {
            val responseBody = restTemplate.exchange(
                    sourceEnum.url,
                    HttpMethod.GET,
                    null,
                    object : ParameterizedTypeReference<List<GitReleaseEntity>>() {}
            ).body ?: return@runCatching CbResponse.failed(
                    code = RESPONSE_CODE_FAILED_NO_RES,
                    message = "Get Resource from remote failed"
            )
            logger.info("latest(source, canary), responseBody = <$responseBody>")
            val latestRelease = responseBody.firstOrNull {
                val name = it.name ?: ""
                name.startsWith("Release") || (canaryEnable && name.startsWith("Pre Release"))
            } ?: return@runCatching CbResponse.failed(
                    code = RESPONSE_CODE_FAILED_NO_RES,
                    message = "No matched release"
            )
            logger.info("latest(source, canary), latestRelease = <$latestRelease>")
            val asset = latestRelease.assets?.firstOrNull {
                val name = it.name ?: ""
                !it.browser_download_url.isNullOrBlank()
                        && name.endsWith(".apk")
                        && (name.contains("_online") || name.contains("_canary"))
            } ?: return@runCatching CbResponse.failed(code = RESPONSE_CODE_FAILED_NO_RES, message = "No matched asset")
            logger.info("latest(source, canary), asset = <$asset>")
            CbResponse.success(
                    ReleaseEntity(
                            latestVersionName = latestRelease.name.orEmpty(),
                            latestVersionInfo = latestRelease.body.orEmpty(),
                            latestApkName = asset.name.orEmpty(),
                            latestApkDownloadUrl = asset.browser_download_url.orEmpty()
                    )
            )
        }.getOrElse { throwable ->
            throwable.printStackTrace()
            CbResponse.failed(message = throwable.localizedMessage)
        }
    }

    @GetMapping("v1/office/callback")
    fun onGetMicrosoftOnlineCodeCallback(
            @RequestParam(name = "code", defaultValue = "") code: String,
            @RequestParam(name = "redirect_uri", defaultValue = "") redirectUri: String,
    ): CbResponse<*> {
        logger.info("onGetMicrosoftOnlineCodeCallback(code = <$code>, redirectUri = <$redirectUri>)")
        if (redirectUri.isBlank()) {
            return CbResponse.failed(message = "redirectUri is blank")
        }
        val url = "https://login.microsoftonline.com/common/oauth2/v2.0/token"
        logger.info("onGetMicrosoftOnlineCodeCallback(code, redirectUri), url = <$url>")
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_FORM_URLENCODED
        val map = LinkedMultiValueMap<String, String>()
        map.add("client_id", configCache["client_id"])
        map.add("client_secret", configCache["client_secret"])
        map.add("grant_type", "authorization_code")
        map.add("code", code)
        map.add("redirect_uri", redirectUri)
        val request = HttpEntity<MultiValueMap<String, String>>(map, headers)
        val responseBody = restTemplate.postForEntity(
                url,
                request,
                MicrosoftTokenEntity::class.java
        ).body ?: return CbResponse.failed(message = "No token Response")
        logger.info("onGetMicrosoftOnlineCodeCallback(code, redirectUri), responseBody = <$responseBody>")
        val accessToken = responseBody.access_token
        val refreshToken = responseBody.refresh_token
        if (accessToken.isNullOrBlank() || refreshToken.isNullOrBlank()) {
            return CbResponse.failed(message = "Get token failed")
        }
        tokenCache.put("access_token", accessToken, responseBody.expires_in ?: 0L, TimeUnit.SECONDS)
        tokenCache.put("refresh_token", refreshToken)
        tokenCache.put("token_type", responseBody.token_type)
        tokenCache.put("expires_in", responseBody.expires_in.toString())
        tokenCache.put("token_ms", System.currentTimeMillis().toString())
        return CbResponse.success(null, message = "Get token Success")
    }

    @GetMapping("v1/office/refreshToken")
    fun refreshOfficeToken(): CbResponse<*> {
        val refresh_token = tokenCache["refresh_token"]
        if (refresh_token.isNullOrBlank()) {
            return CbResponse.failed(message = "No refresh_token, please auth again")
        }
        val url = "https://login.microsoftonline.com/common/oauth2/v2.0/token"
        logger.info("refreshOfficeToken(), url = <$url>")
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_FORM_URLENCODED
        val map = LinkedMultiValueMap<String, String>()
        map.add("client_id", configCache["client_id"])
        map.add("client_secret", configCache["client_secret"])
        map.add("grant_type", "refresh_token")
        map.add("refresh_token", refresh_token)
        val request = HttpEntity<MultiValueMap<String, String>>(map, headers)
        val responseBody = restTemplate.postForEntity(
                url,
                request,
                MicrosoftTokenEntity::class.java
        ).body ?: return CbResponse.failed(message = "No refresh token Response")
        logger.info("refreshOfficeToken(), responseBody = <$responseBody>")
        val accessToken = responseBody.access_token
        val refreshToken = responseBody.refresh_token
        if (accessToken.isNullOrBlank() || refreshToken.isNullOrBlank()) {
            return CbResponse.failed(message = "Refresh token failed")
        }
        tokenCache.put("access_token", accessToken, responseBody.expires_in ?: 0L, TimeUnit.SECONDS)
        tokenCache.put("refresh_token", refreshToken)
        tokenCache.put("token_type", responseBody.token_type)
        tokenCache.put("expires_in", responseBody.expires_in.toString())
        tokenCache.put("token_ms", System.currentTimeMillis().toString())
        return CbResponse.success(null, message = "Refresh token Success")
    }

    @GetMapping("v1/office/getToken")
    fun queryMicrosoftToken(): String {
        return """
            {
                "access_token": "${tokenCache["access_token"]}",
                "refresh_token": "${tokenCache["refresh_token"]}",
                "token_type": "${tokenCache["token_type"]}",
                "expires_in": "${tokenCache["expires_in"]}",
                "token_ms": "${tokenCache["token_ms"]}"
            }
        """.trimIndent()
    }

    @GetMapping("v1/config/update")
    fun updateConfig(
            @RequestParam(name = "client_id", defaultValue = "") clientId: String,
            @RequestParam(name = "client_secret", defaultValue = "") clientSecret: String,
    ): CbResponse<*> {
        if (clientId.isNotBlank()) {
            configCache.put("client_id", clientId)
        }
        if (clientSecret.isNotBlank()) {
            configCache.put("client_secret", clientSecret)
        }
        return CbResponse.success(null)
    }

    @GetMapping("v1/config/all")
    fun queryConfig(): String {
        return """
            {
                "client_id": "${configCache["client_id"]}",
                "client_secret": "${configCache["client_secret"]}",
            }
        """.trimIndent()
    }

    @GetMapping("v1/office/drive/list")
    fun officeDriveList(): CbResponse<*> {
        val url = "https://graph.microsoft.com/v1.0/me/drive/root/children"
        logger.info("officeDriver(), url = <$url>")
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers["Authorization"] = "${tokenCache["token_type"]} ${tokenCache["access_token"]}"
        val request = HttpEntity<MultiValueMap<String, String>>(headers)
        val responseBody = restTemplate.exchange(
                url,
                HttpMethod.GET,
                request,
                DriveEntity::class.java
        ).body ?: return CbResponse.failed(message = "No Response")
        return CbResponse.success(responseBody)
    }

    @GetMapping("v1/office/drive/getById")
    fun officeDriveList(
            @RequestParam(name = "drive_id", defaultValue = "") driveId: String,
            @RequestParam(name = "item_id", defaultValue = "") itemId: String,
    ): CbResponse<*> {
        val url = "https://graph.microsoft.com/v1.0/drives/${driveId}/items/${itemId}/children"
        logger.info("officeDriveList(), url = <$url>")
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers["Authorization"] = "${tokenCache["token_type"]} ${tokenCache["access_token"]}"
        val request = HttpEntity<MultiValueMap<String, String>>(headers)
        val response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                request,
                DriveEntity::class.java
        )
        logger.info("officeDriveList(), response.statusCode = <${response.statusCode}>")
        if (response.statusCode.value() == 401) {
            // 授权失效，尝试刷新token
            val refreshOfficeTokenResponse = refreshOfficeToken()
            return if (refreshOfficeTokenResponse.success) {
                // 刷新 token 成功，重试
                officeDriveList(driveId, itemId)
            } else {
                // 刷新token失败
                refreshOfficeTokenResponse
            }
        }
        val responseBody = response.body ?: return CbResponse.failed(message = "No Response")
        return CbResponse.success(responseBody)
    }

    @GetMapping("v1/office/drive/getById1")
    fun officeDriveList1(
            @RequestParam(name = "drive_id", defaultValue = "") driveId: String,
            @RequestParam(name = "item_id", defaultValue = "") itemId: String,
    ): CbResponse<*> {
        val url = "https://graph.microsoft.com/v1.0/drives/${driveId}/items/${itemId}/children"
        logger.info("officeDriveList1(), url = <$url>")
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers["Authorization"] = "${tokenCache["token_type"]} ${tokenCache["access_token"]}"
        val request = HttpEntity<MultiValueMap<String, String>>(headers)
        val responseBody = restTemplate.exchange(
                url,
                HttpMethod.GET,
                request,
                String::class.java
        ).body ?: return CbResponse.failed(message = "No Response")
        return CbResponse.success(responseBody)
    }

    @GetMapping("v1/office/drive/downloadUrl")
    fun officeDriveDownloadUrl(
            @RequestParam(name = "item_id", defaultValue = "") itemId: String,
    ): CbResponse<*> {
        val url = "https://graph.microsoft.com/v1.0/me/drive/items/${itemId}/content"
        logger.info("officeDriveDownloadUrl(), url = <$url>")
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers["Authorization"] = "${tokenCache["token_type"]} ${tokenCache["access_token"]}"
        val request = HttpEntity<MultiValueMap<String, String>>(headers)
        val response = noRedirectRestTemplate.exchange(
                url,
                HttpMethod.GET,
                request,
                String::class.java
        )
        logger.info("officeDriveDownloadUrl(), response.statusCode = <${response.statusCode}>, response.body = <${response.body}>")
        if (response.statusCode.value() == 401) {
            // 授权失效，尝试刷新token
            val refreshOfficeTokenResponse = refreshOfficeToken()
            return if (refreshOfficeTokenResponse.success) {
                // 刷新 token 成功，重试
                officeDriveDownloadUrl(itemId)
            } else {
                // 刷新token失败
                refreshOfficeTokenResponse
            }
        }
        if (response.statusCode.value() == 302) {
            val location = response.headers.location?.toString()
            if (!location.isNullOrBlank()) {
               return CbResponse.success(location)
            }
        }
        return CbResponse.failed(message = "Get download path failed")
    }
}