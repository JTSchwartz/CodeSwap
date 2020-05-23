package com.jtschwartz.codeswap

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange

abstract class CodeSwapConcept(open var needsSelection: Boolean = true): AnAction() {
	companion object {
		lateinit var anchorRange: TextRange
		lateinit var swapRange: TextRange
		val isAnchorFirst = { anchorRange.startOffset < swapRange.startOffset }
		var doc: Document? = null
		var editor: Editor?  = null
		var project: Project?  = null
	}
	
	override fun update(e: AnActionEvent) {
		project = e.project
		editor = e.getData(CommonDataKeys.EDITOR)
		doc = editor?.document
		
		e.presentation.isVisible = project != null && editor != null
		e.presentation.isEnabled = e.presentation.isVisible && if (needsSelection) editor!!.selectionModel.hasSelection() else true
	}
	
	protected infix fun getSelectionRangeFrom(editor: Editor): TextRange {
		if (needsSelection) return TextRange(editor.selectionModel.selectionStart, editor.selectionModel.selectionEnd)
		
		return TextRange(doc!!.getLineStartOffset(doc!!.getLineNumber(editor.selectionModel.selectionStart)), doc!!.getLineEndOffset(doc!!.getLineNumber(editor.selectionModel.selectionEnd)))
	}
}

open class SoftCodeAnchor: CodeSwapConcept() {
	override fun actionPerformed(e: AnActionEvent) {
		anchorRange = this getSelectionRangeFrom editor!!
	}
}

open class SoftCodeSwap: CodeSwapConcept() {
	override fun actionPerformed(e: AnActionEvent) {
		swapRange = this getSelectionRangeFrom editor!!
		editor!!.caretModel.primaryCaret.removeSelection()
		
		if (doSelectionsOverlap()) return
		
		if (anchorRange.endOffset >= doc!!.textLength) anchorRange = TextRange(anchorRange.startOffset, doc!!.textLength)
		
		val anchorText = doc!!.getText(anchorRange)
		val swapText = doc!!.getText(swapRange)
		
		WriteCommandAction.runWriteCommandAction(project) {
			anchorRange = if (isAnchorFirst()) {
				doc!!.replaceString(swapRange.startOffset, swapRange.endOffset, anchorText)
				doc!!.replaceString(anchorRange.startOffset, anchorRange.endOffset, swapText)
				TextRange(swapRange.startOffset + (swapText.length - anchorText.length), swapRange.startOffset + swapText.length)
			} else {
				doc!!.replaceString(anchorRange.startOffset, anchorRange.endOffset, swapText)
				doc!!.replaceString(swapRange.startOffset, swapRange.endOffset, anchorText)
				TextRange(swapRange.startOffset, swapRange.startOffset + anchorText.length)
			}
		}
	}
	
	private fun doSelectionsOverlap(): Boolean {
		if (isAnchorFirst()) return anchorRange.endOffset > swapRange.startOffset && anchorRange.endOffset > swapRange.endOffset
		return anchorRange.endOffset < swapRange.startOffset && anchorRange.endOffset < swapRange.endOffset
	}
}

class HardCodeAnchor(override var needsSelection: Boolean = false): SoftCodeAnchor()
class HardCodeSwap(override var needsSelection: Boolean = false): SoftCodeSwap()