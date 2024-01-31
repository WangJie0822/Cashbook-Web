package cn.wj.spring.cashbook.entity

class DriveEntity(
    val value: List<DriveItemEntity>?
)

data class DriveItemEntity(
    val id: String,
    val name: String,
    val lastModifiedDateTime: String,
    val folder: DriveItemFolderEntity?,
    val file: DriveItemFileEntity?,
)

data class DriveItemFolderEntity(
    val childCount: Int
)

data class DriveItemFileEntity(
    val mimeType: String
)