plugins {
    id("org.jetbrains.kotlin.jvm")
    application
}

kotlin { jvmToolchain(17) }

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test { useJUnitPlatform() }

application {
    mainClass.set("com.homegame.mahjong.engine.sim.SimulatorKt")
}
