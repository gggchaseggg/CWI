package com.daniilgrachev.cwi.generators

import com.intellij.openapi.ui.InputValidator

class NameValidator : InputValidator {
    override fun checkInput(inputString: String?): Boolean = !inputString.isNullOrBlank()
    override fun canClose(inputString: String?): Boolean = checkInput(inputString)
}