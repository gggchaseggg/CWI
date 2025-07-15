package com.daniilgrachev.cwi.generators

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager

abstract class GeneratorsBase(suffix: String = "", fileExtension: String = "ts") : AnAction() {
    val INDEX_NAME = "index.ts"
    val FILE_EXTENSION = fileExtension
    val FILE_TYPE = FileTypeManager.getInstance().getFileTypeByExtension(FILE_EXTENSION)
    val SUFFIX = suffix

    // -------------- Необходимый метод для AnAction --------------
    override fun actionPerformed(actionEvent: AnActionEvent) {
        val project = actionEvent.project ?: return
        val view = actionEvent.getData(CommonDataKeys.NAVIGATABLE) ?: return
        val rootDir: PsiDirectory = when (view) {
            is PsiDirectory -> view
            is PsiFile -> view.parent
            else -> actionEvent.getData(CommonDataKeys.VIRTUAL_FILE)?.let {
                PsiManager.getInstance(project).findDirectory(it)
            }
        } ?: return

        // Шаг 1: показать диалог конкретного генератора
        val params: DialogResult = showDialog(project) ?: return   // пользователь нажал Cancel

        // Шаг 2: выполнить генерацию в write‑action
        WriteCommandAction.runWriteCommandAction(project) {
            val capitalizedEntityName = capitalize(params.name)
            val dir = getPlaceDir(rootDir, capitalizedEntityName, params)
            runGeneration(project, rootDir, params, capitalizedEntityName, dir)
        }
    }

    // -------------- Методы моего базового класса --------------

    /** Структура данных, которую диалог вернёт в потомке */
    interface DialogResult {
        val name: String
        val needExtraFolder: Boolean
    }

    /** Показываем своё диалоговое окно, возвращаем параметры или null при Cancel */
    protected abstract fun showDialog(project: Project): DialogResult?

    /** Функция генерации */
    protected abstract fun runGeneration(
        project: Project,
        rootDir: PsiDirectory,
        params: DialogResult,
        capitalizedEntityName: String,
        dir: PsiDirectory
    )

    fun capitalize(name: String): String {
        return name.replaceFirstChar { if (it.isLowerCase()) it.uppercase() else it.toString() }
    }


    fun openFile(project: Project, fileName: String, dir: PsiDirectory) {
        dir.findFile(fileName)?.virtualFile?.let {
            FileEditorManager.getInstance(project).openFile(it, true)
        }
    }

    fun getFileName(fileName: String, customSuffix: String = "", customExtension: String = ""): String {
        val constructorSuffix = SUFFIX.lowercase()
        val suffix =
            if (customSuffix != "") ".${customSuffix}"
            else if (constructorSuffix != "") ".${constructorSuffix}"
            else ""

        val extension =
            if (customExtension != "") customExtension
            else FILE_EXTENSION

        return "${fileName}${suffix}.${extension}"

    }

    fun getPlaceDir(rootDir: PsiDirectory, fileName: String, params: DialogResult): PsiDirectory {
        if (params.needExtraFolder) {
            return rootDir.findSubdirectory(fileName) ?: rootDir.createSubdirectory(fileName + SUFFIX)
        }

        return rootDir
    }

    fun createFileWithContent(project: Project, dir: PsiDirectory, fileName: String, fileContent: String) {
        //TODO: generate index file with "ts" type
        val file = PsiFileFactory.getInstance(project)
            .createFileFromText(fileName, FILE_TYPE, fileContent)

        dir.add(file)
    }

    fun getExportLine(name: String, isDefault: Boolean = false): String {
        val suffix = SUFFIX.lowercase()
        val fromEnd = "from './${name}${if (suffix == "") "" else ".${suffix}"}'"

        if (isDefault) return "export { default as ${name} } from './${name}'"

        return "export * ${fromEnd}"
    }


    fun updateIndexFile(file: PsiFile, additionalText: String) {
        val newText = file.text + additionalText
        file.viewProvider.document?.setText(newText)
    }

    fun generateIndexFile(project: Project, dir: PsiDirectory, indexFile: PsiFile?, exportLine: String) {
        if (indexFile == null) {
            createFileWithContent(project, dir, INDEX_NAME, "${exportLine}\n")
        } else {
            if (indexFile.text.contains(exportLine)) return

            updateIndexFile(indexFile, "\n${exportLine}\n")
        }
    }

    fun generateIndexFiles(
        project: Project,
        name: String,
        dir: PsiDirectory,
        params: DialogResult,
        isDefault: Boolean = false
    ) {

        val indexFile = dir.findFile(INDEX_NAME)
        val exportLine = getExportLine(name, isDefault)

        generateIndexFile(project, dir, indexFile, exportLine)


        if (params.needExtraFolder) {
            val parentDir = dir.parentDirectory ?: return
            val parentDirIndexFile = parentDir.findFile(INDEX_NAME)
            val exportFolderLine = getExportLine(name + SUFFIX)

            generateIndexFile(project, parentDir, parentDirIndexFile, exportFolderLine)
        }
    }
}