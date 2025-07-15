package com.daniilgrachev.cwi.generators

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiDirectory

data class ServiceDialogResult(
    override val name: String,
    override val needExtraFolder: Boolean
) : GeneratorsBase.DialogResult

class ServiceGenerator : GeneratorsBase(suffix = "Service") {
    override fun showDialog(project: Project): DialogResult? {
        val value = Messages.showInputDialogWithCheckBox(
            "",
            "Create Service Class",
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

        return ServiceDialogResult(storeName, needExtraFolder)
    }

    override fun runGeneration(
        project: Project,
        rootDir: PsiDirectory,
        params: DialogResult,
        capitalizedEntityName: String,
        dir: PsiDirectory
    ) {
        // ---------- Файл ----------
        val fileName = getFileName(capitalizedEntityName)
        // -------------- Контент --------------
        val fileContent = """            
            export class ${capitalizedEntityName}Service {
                
            }

        """.trimIndent()

        createFileWithContent(project, dir, fileName, fileContent)

        // ---------- index.ts: создать или дополнить ----------
        generateIndexFiles(project, capitalizedEntityName, dir, params)

        // ---------- Открываем созданный файл в редакторе ----------
        openFile(project, fileName, dir)
    }
}