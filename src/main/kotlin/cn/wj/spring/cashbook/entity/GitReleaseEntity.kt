/*
 * Copyright 2021 The Cashbook Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.wj.spring.cashbook.entity

/**
 * Git 仓库 Release 信息
 *
 * @param name 版本名
 * @param body 描述信息
 * @param assets 资产列表
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/20
 */
data class GitReleaseEntity(
    val id: Long? = null,
    val name: String? = null,
    val body: String? = null,
    val assets: List<GitReleaseAssetEntity>? = null,
)

/**
 * 资产信息
 *
 * @param name 名称
 * @param browser_download_url 下载地址
 */
data class GitReleaseAssetEntity(
    val name: String? = null,
    val browser_download_url: String? = null,
)
