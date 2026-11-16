package com.github.shadowsocks.bg

// Имя пакета/класса зашито в нативной библиотеке tun2proxy — не менять.
object Tun2proxy {
    init { System.loadLibrary("tun2proxy") }
    @JvmStatic external fun run(cliArgs: String, tunMtu: Char): Int
    @JvmStatic external fun stop(): Int
}
