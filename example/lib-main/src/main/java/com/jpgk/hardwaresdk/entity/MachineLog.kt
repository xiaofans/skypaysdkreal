package com.jpgk.vendingmachine.lib_base.entity

class MachineLog {
    //货道编号
    var aisleNumber: String? = null

    //1:通讯失败 2:系统内存不足 3：文件系统异常(创建数据库失败) 4：连接服务器失败 5 ：缺货
    // 6 ：下发广告失败 7 播放视频失败 8：未绑定货道 9：纸币器故障 10：硬币器故障 11： 传感器故障
    // 12：货道出货故障 13：断线故障 14：过载故障 15： 电机异常 16：红外检测异常 17：未检测到停止信号
    // 18:设备上线，19：设备下线 20：未知原因故障 21:整机故障  22:超时
    var logType: Int? = null

    //售货机货道ID标识
    var machineAisleId: Int? = null

    //售卖机ID标识
    var machineId: Int? = null
    override fun toString(): String {
        return "MachineLog(aisleNumber=$aisleNumber, logType=$logType, machineAisleId=$machineAisleId, machineId=$machineId)"
    }


}