package com.daniilgrachev.cwi.generators

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.InputValidator
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager

class CreateStoreAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val view = e.getData(CommonDataKeys.NAVIGATABLE) ?: return
        val psiDir = when (view) {
            is PsiDirectory -> view
            else -> e.getData(CommonDataKeys.VIRTUAL_FILE)?.let {
                PsiManager.getInstance(project).findDirectory(it)
            }
        } ?: return

        val value = Messages.showInputDialogWithCheckBox(
            "",
            "Create Mobx Store",
            "Create extra folder",
            false,
            true,
            null,
            "",
            NameValidator()
        )

        val storeName = value.first
        val needExtraFolder = value.second

        if (storeName == null || needExtraFolder == null) return

        WriteCommandAction.runWriteCommandAction(project) {
            generateStoreScaffold(project, psiDir, storeName, needExtraFolder)
        }
    }

    private fun generateStoreScaffold(
        project: Project,
        rootDir: PsiDirectory,
        rawName: String,
        needExtraFolder: Boolean
    ) {
        val capitalized = rawName.replaceFirstChar { if (it.isLowerCase()) it.uppercase() else it.toString() }

        // Если нужна доп. папка — создаём или находим её
        val storeDir = if (needExtraFolder) {
            rootDir.findSubdirectory(capitalized) ?: rootDir.createSubdirectory(capitalized)
        } else {
            rootDir
        }

        // ---------- Файл стора ----------
        val storeFileName = "${capitalized}.store.ts"
        val storeContent = """
            import { makeAutoObservable } from "mobx"
            
            class ${capitalized}Store {
                constructor() {
                    makeAutoObservable(this)   
                }
            }

            export const ${capitalized.replaceFirstChar { it.lowercase() }}Store = new ${capitalized}Store()
        """.trimIndent()

        val storePsi = PsiFileFactory.getInstance(project)
            .createFileFromText(storeFileName, storeContent)
        storeDir.add(storePsi)


        // ---------- index.ts: создать или дополнить ----------
        val indexFileName = "index.ts"
        val exportLine = "export * from './${capitalized}.store'"
        val indexPsi = storeDir.findFile(indexFileName)

        if (indexPsi == null) {
            val indexPsiNew = PsiFileFactory.getInstance(project)
                .createFileFromText(indexFileName, "$exportLine\n")

            storeDir.add(indexPsiNew)
        } else if (!indexPsi.text.contains(exportLine)) {
            val newText = indexPsi.text + "\n$exportLine\n"
            indexPsi.viewProvider.document?.setText(newText)
        }

        if (needExtraFolder) {
            // --------------Создание индекса в папке ---------------
            val rootIndexPsi = rootDir.findFile(indexFileName)
            val exportFolderLine = "export * from './${capitalized}'"

            if (rootIndexPsi == null) {
                val indexPsiNew = PsiFileFactory.getInstance(project)
                    .createFileFromText(indexFileName, "$exportFolderLine\n")

                rootDir.add(indexPsiNew)
            } else if (!rootIndexPsi.text.contains(exportFolderLine)) {
                val newText = rootIndexPsi.text + "\n$exportFolderLine\n"
                rootIndexPsi.viewProvider.document?.setText(newText)
            }
        }

        // ---------- Открываем созданный store в редакторе ----------
        storeDir.findFile(storeFileName)?.virtualFile?.let {
            FileEditorManager.getInstance(project).openFile(it, true)
        }
    }

    private class NameValidator : InputValidator {
        override fun checkInput(inputString: String?): Boolean = !inputString.isNullOrBlank()
        override fun canClose(inputString: String?): Boolean = checkInput(inputString)
    }
}