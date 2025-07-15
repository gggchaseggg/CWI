package com.daniilgrachev.cwi.generators

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiDirectory

data class ZustandStoreDialogResult(
    override val name: String,
    override val needExtraFolder: Boolean
) : GeneratorsBase.DialogResult

class ZustandStoreGenerator : GeneratorsBase(suffix = "Store") {
    override fun showDialog(project: Project): DialogResult? {
        val value = Messages.showInputDialogWithCheckBox(
            "",
            "Create Zustand Store",
            "Create extra folder",
            false,
            true,
            null,
            "",
            NameValidator()
        )

        val storeName = value.first
        val needExtraFolder = value.second

        if (storeName == null || needExtraFolder == null) return null

        return ZustandStoreDialogResult(storeName, needExtraFolder)
    }

    override fun runGeneration(
        project: Project,
        rootDir: PsiDirectory,
        params: DialogResult,
        capitalizedEntityName: String,
        dir: PsiDirectory
    ) {
        // ---------- Файл стора ----------
        val storeFileName = getFileName(capitalizedEntityName)
        // -------------- Контент стора --------------
        val storeContent = """
            import { create } from 'zustand'
            
            const use${capitalizedEntityName}Store = create((set, get) => ({
                
            }))
            
            export const useState = use${capitalizedEntityName}Store((state) => state)
        """.trimIndent()

        createFileWithContent(project, dir, storeFileName, storeContent)

        // ---------- index.ts: создать или дополнить ----------
        generateIndexFiles(project, capitalizedEntityName, dir, params)

        // ---------- Открываем созданный store в редакторе ----------
        openFile(project, storeFileName, dir)
    }
}