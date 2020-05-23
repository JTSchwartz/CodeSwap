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
		var doc: Document? = null
		var editor: Editor?  = null
		var project: Project?  = null
	}
	
	override fun update(e: AnActionEvent) {
		project = e.project
		editor = e.getData(CommonDataKeys.EDITOR)
		doc = editor?.document
		
		e.presentation.isVisible = if (project != null && editor != null) true else return
		e.presentation.isEnabled = if (needsSelection) editor!!.selectionModel.hasSelection() else true
	}
	
	protected fun getSelectionRange(editor: Editor, isSoftSelection: Boolean): TextRange {
		if (isSoftSelection) return TextRange(editor.selectionModel.selectionStart, editor.selectionModel.selectionEnd)
		
		return TextRange(doc!!.getLineStartOffset(doc!!.getLineNumber(editor.selectionModel.selectionStart)), doc!!.getLineEndOffset(doc!!.getLineNumber(editor.selectionModel.selectionEnd)))
	}
}

open class HardCodeAnchor(override var needsSelection: Boolean = false): CodeSwapConcept() {
	override fun actionPerformed(e: AnActionEvent) {
		anchorRange = editor?.let {getSelectionRange(it, this is SoftCodeAnchor)}?: return
	}
}

open class HardCodeSwap(override var needsSelection: Boolean = false): CodeSwapConcept() {
	override fun actionPerformed(e: AnActionEvent) {
		swapRange = editor?.let {getSelectionRange(it, this is SoftCodeSwap)}?: return
		
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
		
		editor!!.caretModel.primaryCaret.removeSelection()
	}
	
	private fun doSelectionsOverlap(): Boolean {
		if (isAnchorFirst()) return anchorRange.endOffset > swapRange.startOffset && anchorRange.endOffset > swapRange.endOffset
		return anchorRange.endOffset < swapRange.startOffset && anchorRange.endOffset < swapRange.endOffset
	}
	
	private fun isAnchorFirst(): Boolean {
		return anchorRange.startOffset < swapRange.startOffset
	}
}

class SoftCodeAnchor(override var needsSelection: Boolean = true): HardCodeAnchor()
class SoftCodeSwap(override var needsSelection: Boolean = true): HardCodeSwap()