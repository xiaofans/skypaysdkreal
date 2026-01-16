package com.jpgk.vendingmachine.lib_base.entity

/**
 * 正在出货数据
 */
class OutingEntity {
    //货柜id
    var machineId:Int?=null
    //货道id
    var aisleId:Int?=null
    //货柜地址
    var machineAddress:Int?=null
    //货道地址
    var aisleAddress:Int?=null
    override fun toString(): String {
        return "OutingEntity(machineId=$machineId, aisleId=$aisleId, machineAddress=$machineAddress, aisleAddress=$aisleAddress)"
    }


}