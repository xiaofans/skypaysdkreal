package com.jpgk.hardwaresdk.hardwareprotocolimpl

fun parseCoinAcceptorSetup(response: String): CoinAcceptorConfig {
    // Convert HEX string to byte array
    val hexString = response.replace(" ", "").trim()
    val bytes = hexString.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    // Extract values from the byte array
    val featureLevel = bytes[0].toInt()
    val countryCode = ((bytes[1].toInt() and 0xFF) shl 8) or (bytes[2].toInt() and 0xFF)
    val scalingFactor = bytes[3].toInt()
    val decimalPlaces = bytes[4].toInt()
    val coinRouting = ((bytes[5].toInt() and 0xFF) shl 8) or (bytes[6].toInt() and 0xFF)
    val coinCredits = bytes.slice(7..22).map { it.toInt() and 0xFF }

    // Create a configuration object
    return CoinAcceptorConfig(
        featureLevel = featureLevel,
        countryCode = countryCode,
        scalingFactor = scalingFactor,
        decimalPlaces = decimalPlaces,
        coinRouting = coinRouting,
        coinCredits = coinCredits
    )
}

data class CoinAcceptorConfig(
    val featureLevel: Int,
    val countryCode: Int,
    val scalingFactor: Int,
    val decimalPlaces: Int,
    val coinRouting: Int,
    val coinCredits: List<Int>
)

fun main() {
    val response = "3033203139203031203031203030203030203137203031203035203041203134203332203030203030203030203030203030203030203030203030203030203030203030203842200D0A"
    val config = parseCoinAcceptorSetup(response)
    println(config)
}
