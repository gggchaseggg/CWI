package com.daniilgrachev.cwi.generators

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.psi.PsiDirectory
import java.awt.BorderLayout
import javax.swing.*


data class ReactDialogResult(
    override val name: String,
    override val needExtraFolder: Boolean,
    val needTypes: Boolean
) : GeneratorsBase.DialogResult

class ReactGenerator : GeneratorsBase(fileExtension = "tsx") {
    override fun showDialog(project: Project): DialogResult? {
        val nameField = JTextField(30)
        val folderCheckbox = JCheckBox("Create extra folder")
        val typesCheckbox = JCheckBox("Generate types file")

        fun wrapWest(comp: JComponent) = JPanel(BorderLayout()).apply {
            isOpaque = false
            add(comp, BorderLayout.WEST)
        }

        val panel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(nameField)
            add(Box.createVerticalStrut(12))
            add(wrapWest(folderCheckbox))
            add(wrapWest(typesCheckbox))
        }

        return object : DialogWrapper(project, true) {
            init {
                title = "Create React Component"
                init()
            }

            override fun getPreferredFocusedComponent(): JComponent = nameField

            override fun createCenterPanel(): JComponent = panel
        }.run {
            if (showAndGet()) {
                val raw = nameField.text.trim()
                if (raw.isEmpty()) null
                else ReactDialogResult(
                    name = raw,
                    needExtraFolder = folderCheckbox.isSelected,
                    needTypes = typesCheckbox.isSelected
                )
            } else null
        }

    }

    override fun runGeneration(
        project: Project,
        rootDir: PsiDirectory,
        params: DialogResult,
        capitalizedEntityName: String,
        dir: PsiDirectory
    ) {
        val (_, _, needTypes) = params as ReactDialogResult

        val fileName = getFileName(capitalizedEntityName)

        val propsType = "${capitalizedEntityName}Props"

        val fileContent = """            
            import type { FC } from 'react'
            ${if (needTypes) "import type { ${propsType} } from './${capitalizedEntityName}.types'" else ""}

            const ${capitalizedEntityName}: FC${if (needTypes) "<${propsType}>" else ""} = () => {
                return (
                    <div>

                    </div>
                )
            }

            export default ${capitalizedEntityName}
        """.trimIndent()

        if (needTypes) {
            val typesFileName = getFileName(capitalizedEntityName, "types", "ts")

            val typesFileContent = """            
                export type ${capitalizedEntityName}Props = {
                    
                }
                
            """.trimIndent()

            createFileWithContent(project, dir, typesFileName, typesFileContent)

            openFile(project, typesFileName, dir)
        }

        createFileWithContent(project, dir, fileName, fileContent)

        generateIndexFiles(project, capitalizedEntityName, dir, params, true)

        openFile(project, fileName, dir)
    }
}
