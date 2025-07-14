package com.daniilgrachev.cwi.generators

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiDirectory

data class MobxStoreDialogResult(
    override val name: String,
    override val needExtraFolder: Boolean
) : GeneratorsBase.DialogResult

class MobxStoreGenerator : GeneratorsBase(dirSuffix = "Store") {
    override fun showDialog(project: Project): DialogResult? {
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

        if (storeName == null || needExtraFolder == null) return null

        return MobxStoreDialogResult(storeName, needExtraFolder)
    }

    override fun runGeneration(
        project: Project,
        rootDir: PsiDirectory,
        params: DialogResult,
        capitalizedEntityName: String,
        dir: PsiDirectory
    ) {
        // ---------- Файл стора ----------
        val storeFileName = getFileName(capitalizedEntityName, "store")
        // -------------- Контент стора --------------
        val storeContent = """
            import { makeAutoObservable } from 'mobx'
            
            class ${capitalizedEntityName}Store {
                constructor() {
                    makeAutoObservable(this)   
                }
            }

            export const ${capitalizedEntityName.replaceFirstChar { it.lowercase() }}Store = new ${capitalizedEntityName}Store()
        """.trimIndent()

        createFileWithContent(project, dir, storeFileName, storeContent)

        // ---------- index.ts: создать или дополнить ----------
        generateIndexFiles(project, capitalizedEntityName, dir, params)

        // ---------- Открываем созданный store в редакторе ----------
        openFile(project, storeFileName, dir)
    }
}