# ARouterDemo
kotlin版本的ARouter路由


如果遇到报错
w: 警告: 来自注释处理程序 'org.jetbrains.kotlin.kapt3.base.ProcessorWrapper' 的受支持 source 版本 'RELEASE_7' 低于 -source '1.8'

添加代码

 compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_7
        targetCompatibility JavaVersion.VERSION_1_7
    }