package com.model.buildgradle

import org.gradle.api.Plugin
import org.gradle.api.Project

class ModelPlugin implements Plugin<Project> {

    //默认是app，直接运行assembleRelease的时候，等同于运行app:assembleRelease
    String compilemodule = "app"

    void apply(Project project) {
        String taskNames = project.gradle.startParameter.taskNames.toString()
        System.out.println("taskNames is " + taskNames)
        String module = project.path.replace(":", "")
        System.out.println("current module is " + module)
        AssembleTask assembleTask = getTaskInfo(project.gradle.startParameter.taskNames)
        if (assembleTask.isAssemble) {
            fetchMainModulename(project, assembleTask)
            System.out.println("compilemodule  is " + compilemodule)
        }

        if (!project.hasProperty("RunAlone")) {
            throw new RuntimeException("you should set isRunAlone in " + module + "'s gradle.properties")
        }

        //对于isRunAlone==true的情况需要根据实际情况修改其值，
        // 但如果是false，则不用修改
        boolean isRunAlone = Boolean.parseBoolean((project.properties.get("RunAlone")))
        String mainmodulename = project.rootProject.property("mainmodulename")
        if (isRunAlone && assembleTask.isAssemble) {
            //对于要编译的组件和主项目，isRunAlone修改为true，其他组件都强制修改为false
            //这就意味着组件不能引用主项目，这在层级结构里面也是这么规定的
            if (module.equals(compilemodule) || module.equals(mainmodulename)) {
                isRunAlone = true
            } else {
                isRunAlone = false
            }
        }
        project.setProperty("RunAlone", isRunAlone)

        //根据配置添加各种组件依赖，并且自动化生成组件加载代码
        if (isRunAlone) {
            project.apply plugin: 'com.android.application'
            if (!module.equals(mainmodulename)) {
                project.android.sourceSets {
                    main {
                        manifest.srcFile 'src/main/independent/AndroidManifest.xml'
                        java.srcDirs = ['src/main/java', 'src/main/independent/java']
                        res.srcDirs = ['src/main/res', 'src/main/independent/res']
                    }
                }
            }
            System.out.println("apply plugin is " + 'com.android.application')
            if (assembleTask.isAssemble) {
                if(module.equals(mainmodulename)){
                    compileAppComponents(assembleTask, project)
                }else{
                    compileComponents(assembleTask, project)
                }
            }
        } else {
            project.apply plugin: 'com.android.library'
            if (!module.equals(mainmodulename)) {
                project.android.sourceSets {
                    main {
                        manifest.srcFile 'src/main/AndroidManifest.xml'
                        java {
                            exclude 'src/main/independent/**'
                            exclude '**/independent/**.java'
                            exclude 'src/main/independent/**/res/layout/activity_independent.xml'
                        }
                    }
                }
            }
            System.out.println("apply plugin is " + 'com.android.library')
        }

    }

    /**
     * 根据当前的task，获取要运行的组件，规则如下：
     * assembleRelease ---app
     * app:assembleRelease :app:assembleRelease ---app
     * sharecomponent:assembleRelease :sharecomponent:assembleRelease ---sharecomponent
     * @param assembleTask
     */
    private void fetchMainModulename(Project project, AssembleTask assembleTask) {
        if (!project.rootProject.hasProperty("mainmodulename")) {
            throw new RuntimeException("you should set compilemodule in rootproject's gradle.properties")
        }
        if (assembleTask.modules.size() > 0 && assembleTask.modules.get(0) != null
                && assembleTask.modules.get(0).trim().length() > 0
                && !assembleTask.modules.get(0).equals("all")) {
            compilemodule = assembleTask.modules.get(0)
        } else {
            compilemodule = project.rootProject.property("mainmodulename")
        }
        if (compilemodule == null || compilemodule.trim().length() <= 0) {
            compilemodule = "app"
        }
    }

    private AssembleTask getTaskInfo(List<String> taskNames) {
        AssembleTask assembleTask = new AssembleTask()
        for (String task : taskNames) {
            if (task.toUpperCase().contains("ASSEMBLE")
                    || task.contains("aR")
                    || task.toUpperCase().contains("TINKER")
                    || task.toUpperCase().contains("INSTALL")
                    || task.toUpperCase().contains("RESGUARD")) {
                if (task.toUpperCase().contains("DEBUG")) {
                    assembleTask.isDebug = true
                }
                assembleTask.isAssemble = true
                String[] strs = task.split(":")
                assembleTask.modules.add(strs.length > 1 ? strs[strs.length - 2] : "all")
                break
            }
        }
        return assembleTask
    }

    /**
     * 自动添加依赖，只在运行assemble任务的才会添加依赖，因此在开发期间组件之间是完全感知不到的，这是做到完全隔离的关键
     * 支持两种语法：module或者groupId:artifactId:version(@aar),前者之间引用module工程，后者使用maven中已经发布的aar
     * @param assembleTask
     * @param project
     */
    private void compileAppComponents(AssembleTask assembleTask, Project project) {
        HashMap<String, Project> projectMash = project.rootProject.getChildProjects();
        String mainmodulename  = project.rootProject.property("mainmodulename")
        System.out.println("project mainmodulename== " + mainmodulename)
        ArrayList<Project> taskList = new ArrayList<>();
        for (String key : projectMash.keySet()) {
            System.out.println("project key== " + key)
            if (!key.equals(mainmodulename)
                    && projectMash.get(key).properties.get("RunAlone") != null) {
                System.out.println("project mainmodulename== " + mainmodulename)
                taskList.add(projectMash.get(key));
            }
        }
        System.out.println("project taskList== " + taskList.size())
        for (Project taskPro : taskList) {
            System.out.println("project taskList== " + taskPro.name)
            project.dependencies.add("api", project.project(':' + taskPro.name))
        }
    }

    private void compileComponents(AssembleTask assembleTask, Project project) {
        String components = (String) project.properties.get("component")
        if (components == null || components.length() == 0) {
            System.out.println("there is no add dependencies ")
            return
        }
        String[] compileComponents = components.split(",")
        if (compileComponents == null || compileComponents.length == 0) {
            System.out.println("there is no add dependencies ")
            return
        }
        for (String str : compileComponents) {
            System.out.println("comp is " + str)
            if (str.contains(":")) {
                /**
                 * 示例语法:groupId:artifactId:version(@aar)
                 * compileComponent=com.luojilab.reader:readercomponent:1.0.0
                 * 注意，前提是已经将组件aar文件发布到maven上，并配置了相应的repositories
                 */
                project.dependencies.add("api", str)
                System.out.println("add dependencies lib  : " + str)
            } else {
                /**
                 * 示例语法:module
                 * compileComponent=readercomponent,sharecomponent
                 */
                project.dependencies.add("api", project.project(':' + str))
                System.out.println("add dependencies project : " + str)
            }
        }
    }


    private class AssembleTask {
        boolean isAssemble = false
        boolean isDebug = false
        List<String> modules = new ArrayList<>()
    }


}