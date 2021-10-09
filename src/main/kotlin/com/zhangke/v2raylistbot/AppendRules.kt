package com.zhangke.v2raylistbot

import java.io.File
import java.io.FileNotFoundException

fun main(args: Array<String>?) {
    AppendRules().append(args)
}

class AppendRules {

    companion object {
        private const val CONFIG_FILE = "~/.config/v2raylistbot/config.txt"
    }

    fun append(args: Array<String>?) {
        val command = args.fetchCommand()
        val configuration = getV2rayConfig()
        val v2rayConfigFile = configuration.v2rayConfigFile
        val ruleName = configuration.ruleName
        val lines = v2rayConfigFile.readLines(Charsets.UTF_8).toMutableList()
        val rulesFirstLine = lines.indexOfFirst { it.contains(ruleName) }
        val templateFormat = lines[rulesFirstLine]
        val newRule = replaceDomainWithTemplate(command.domain, templateFormat, configuration.ruleName)
        if (lines.find { it == newRule } != null) {
            println("该规则已存在！")
            return
        }
        lines.add(rulesFirstLine, newRule)
        v2rayConfigFile.writeLines(lines)
    }

    private fun Array<String>?.fetchCommand(): Command {
        val noDomainException = IllegalArgumentException("请输入域名")
        if (this == null || size <= 0) throw noDomainException
        val domain = firstOrNull { it.isNotEmpty() && it.isNotBlank() } ?: throw noDomainException
        return Command(domain)
    }

    private fun File.writeLines(lines: List<String>) {
        val builder = StringBuilder()
        lines.forEach {
            builder.appendLine(it)
        }
        writeText(builder.toString(), Charsets.UTF_8)
    }

    private fun replaceDomainWithTemplate(domain: String, template: String, ruleName: String): String {
        val dirtyText = "$ruleName,(.*),".toRegex().find(template)?.value ?: throw IllegalStateException("字符串匹配失败")
        val oldDomain = dirtyText.replace("$ruleName,", "").replace(",", "")
        return template.replace(oldDomain, domain)
    }

    private fun getV2rayConfig(): Configuration {
        val configFile = requireConfigFile()
        val lines = configFile.readLines(Charsets.UTF_8)
        if (lines.size < 2) {
            throw IllegalStateException("config file is invalid!")
        }
        val filePath = lines.first {
            it.isNotEmpty() && it.isNotBlank()
        }
        val file = filePath.getAbsoluteFile()
        if (!file.exists()) {
            throw FileNotFoundException("${file.absoluteFile} is not found!")
        }
        val ruleName = lines.first {
            it.isNotEmpty() && it.isNotBlank() && !it.contains(filePath)
        }
        if (ruleName.isEmpty()) throw IllegalStateException("rule name is not found!")
        return Configuration(file, ruleName)
    }

    private fun requireConfigFile(): File {
        val configFile = CONFIG_FILE.getAbsoluteFile()
        if (!configFile.exists()) {
            throw FileNotFoundException("${configFile.absoluteFile} is not found!")
        }
        return configFile
    }

    private fun String.getAbsoluteFile(): File {
        if (startsWith("~/")) {
            val homeDir = System.getProperty("user.home")
            return File(replaceFirst("~", homeDir))
        }
        return File(this)
    }
}

data class Command(val domain: String)

data class Configuration(val v2rayConfigFile: File, val ruleName: String)