package com.jpgk.vendingmachine.lib_base.entity

/**
 * 设备信息
 */
data class MachineEntity(
    //审批状态（0：待审批；1：已同意；2：已驳回）
    var auditStatus: Int? = null,
    //审批人ID
    var auditUserId: Int? = null,
    //审批人名称
    var auditUserName: String? = null,
    //创建时间
    var createTime: String? = null,
    //描述
    var description: String? = null,
    //详细地址
    var detailLocation: String? = null,
    //在售商品总数
    var goodsTotalType: Int? = null,
    //职场ID标识
    var lineId: Int? = null,
    //职场名称
    var lineName: String? = null,
    //货道数
    var machineAisleTotal: Int? = null,
    //设备商品信息
    var machineCabinetAisleList: List<MachineCabinetAisle>? = null,
    //识别码，全局唯一
    var machineCode: String? = null,
    //售货机主键
    var machineId: Int? = null,
    //设备型号表
    var machineModelId: Int? = null,
    //机器名称
    var machineName: String? = null,
    //设备签名
    var machineSign: String? = null,
    //120:正常售卖;100:暂停售卖,可人工设置;80:机器故障暂停售卖(系统检测正常后自动恢复售卖);40:已删除
    var machineStatus: Int? = null,
    //在线状态:120:在线;40:离线
    var onlineStatus: Int? = null,
    //组织
    var orgId: Int? = null,
    //组织名称
    var orgName: String? = null,
    //楼层ID标识
    var pointLocationId: Int? = null,
    //城市ID标识
    var regionId: Int? = null,
    //城市名称
    var regionName: String? = null,
    //软件版本(只工控机)
    var softVersion: String? = null,
    //更新时间
    var updateTime: String? = null
)

data class MachineCabinetAisle(
    val aisleNumber: String,
    val balance: Int,
    val capacity: Int,
    val createTime: String,
    val goodsId: Int,
    val machineAisleCol: Int,
    val machineAisleColCell: Int,
    val machineAisleId: Int,
    val machineAisleRow: Int,
    val machineAisleRowCell: Int,
    val machineAisleStatus: Int,
    val machineCabinetId: Int,
    val machineId: Int,
    val mainImage: String,
    val price: Int,
    val timeError: Int,
    val updateTime: String,
    val vmcAddress: String
)