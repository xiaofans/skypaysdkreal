package com.jpgk.hardwaresdk.hardwareprotocolimpl

import com.google.gson.annotations.SerializedName

class DDPayOrderInvoiceItem {
    var Items_SName:String? = null
    var Items_ID:String? = null
    var Items_UnitPrice:String? = null
    var Items_OrignPrice:String? = null
    var Items_Quantity:String? = null
    var Items_UnitAmount:String? = null
    @SerializedName("unit")
    var unit_wrapper:String? = null
    var extra:String? = null
    var Items_Taxrate:String? = null
    var Items_Taxtype:String? = null
}
