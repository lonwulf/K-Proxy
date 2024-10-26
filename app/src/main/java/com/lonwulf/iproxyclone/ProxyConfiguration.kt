package com.lonwulf.iproxyclone

data class ProxyConfiguration(
    var host: String = "",
    var port: Int = 0,
    var proxyType: ProxyType = ProxyType.HTTP,
    var username: String? = null,
    var password: String? = null
)

enum class ProxyType{
    HTTP, SOCKS5
}
