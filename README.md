博客地址 https://blog.csdn.net/Apple_hsp/article/details/105184617

Gradle脚本的使用
模块边界,代码隔离
最后
组件化:Gradle脚本的使用
SetUp.1 : 主项目引用编译脚本

在根目录的gradle.properties文件中，增加属性：

mainmodulename=app

其中mainmodulename是项目中的host工程，一般为app

SetUp.2 : 在根目录的gradle文件中配置

    repositories {

            jcenter()
        }
        dependencies {
            classpath 'com.android.tools.build:gradle:3.0.1'
            .....
            classpath 'com.model.buildgradle:model-plugin:1.0.0'//添加组件化插件
            .....
        }
    }

    allprojects {
        repositories {
                .....
        }
    }
    .....
SetUp.3 : 拆分组件为module工程

在非app组件的工程目录下新建文件gradle.properties文件，增加以下配置：

RunAlone=true
component=sharecomponent,other(其他模块)
在app组件的工程目录下新建文件gradle.properties文件，增加以下配置,默认自动添加所有其它组件： RunAlone=true

上面三个属性分别对应

RunAlone : 否单独调试
component :组件化模式下依赖的组件
SetUp.4 : 在组件和host的build.gradle都增加配置

apply plugin: 'model-plugin'
//注意：不需要在引用com.android.application或者com.android.library
组件化:模块边界,代码隔离
在项目中我使用的是阿里的Arouter方案,对模块之间Activity的和Fragment的跳转,以及接口应用

组件化插件源码github:https://github.com/apple317/ModelPlugin
