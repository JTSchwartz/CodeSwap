package com.jtschwartz.codeswap

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange

lateinit var anchorRange: TextRange

private fun getSelectionRange(editor: Editor, isHardSelection: Boolean): TextRange {
	if (isHardSelection) {
		val doc = editor.document
		
		val selectionStartLine = doc.getLineNumber(editor.selectionModel.selectionStart)
		val selectionEndLine = doc.getLineNumber(editor.selectionModel.selectionEnd)
		
		return TextRange(doc.getLineStartOffset(selectionStartLine), doc.getLineEndOffset(selectionEndLine))
	}
	
	return TextRange(editor.selectionModel.selectionStart, editor.selectionModel.selectionEnd)
}

private fun makeVisibleAndCheckCapability(e: AnActionEvent, needsSelection: Boolean) {
	e.presentation.isVisible = true
	
	val project = e.project
	val editor = e.getData(CommonDataKeys.EDITOR)
	
	e.presentation.isEnabled = project != null
			&& editor != null
			&& editor.caretModel.caretCount == 1
			&& if (needsSelection) editor.selectionModel.hasSelection() else true
}

open class SoftCodeAnchor: AnAction() {
	
	override fun actionPerformed(e: AnActionEvent) {
		anchorRange = getSelectionRange(e.getData(CommonDataKeys.EDITOR) ?: return, false)
	}
	
	override fun update(e: AnActionEvent) {
		makeVisibleAndCheckCapability(e, true)
	}
}

class HardCodeAnchor: AnAction() {
	
	override fun actionPerformed(e: AnActionEvent) {
		anchorRange = getSelectionRange(e.getData(CommonDataKeys.EDITOR) ?: return, true)
	}
	
	override fun update(e: AnActionEvent) {
		makeVisibleAndCheckCapability(e, false)
	}
}

open class SoftCodeSwap: AnAction() {
	lateinit var swapRange: TextRange
	
	override fun actionPerformed(e: AnActionEvent) {
		this.swapRange = getSelectionRange(e.getData(CommonDataKeys.EDITOR) ?: return, false)
		
		if (doSelectionsOverlap()) return
		
		performSwap(e)
	}
	
	protected fun doSelectionsOverlap(): Boolean {
		if (isAnchorFirst()) return anchorRange.endOffset > swapRange.startOffset && anchorRange.endOffset > swapRange.endOffset
		return anchorRange.endOffset < swapRange.startOffset && anchorRange.endOffset < swapRange.endOffset
	}
	
	private fun isAnchorFirst(): Boolean {
		return anchorRange.startOffset < swapRange.startOffset
	}
	
	protected fun performSwap(e: AnActionEvent) {
		val editor = e.getData(CommonDataKeys.EDITOR)!!
		val doc = editor.document
		val project = e.getRequiredData(CommonDataKeys.PROJECT)
		val anchorText = doc.getText(anchorRange)
		val swapText = doc.getText(swapRange)
		
		WriteCommandAction.runWriteCommandAction(project) {
			if (isAnchorFirst()) {
				doc.replaceString(swapRange.startOffset, swapRange.endOffset, anchorText)
				doc.replaceString(anchorRange.startOffset, anchorRange.endOffset, swapText)
			} else {
				doc.replaceString(anchorRange.startOffset, anchorRange.endOffset, swapText)
				doc.replaceString(swapRange.startOffset, swapRange.endOffset, anchorText)
			}
		}
		
		editor.caretModel.primaryCaret.removeSelection()
	}
	
	override fun update(e: AnActionEvent) {
		makeVisibleAndCheckCapability(e, true)
	}
}

class HardCodeSwap: SoftCodeSwap() {
	
	override fun actionPerformed(e: AnActionEvent) {
		this.swapRange = getSelectionRange(e.getData(CommonDataKeys.EDITOR) ?: return, true)
		
		if (doSelectionsOverlap()) return
		
		performSwap(e)
	}
	
	override fun update(e: AnActionEvent) {
		makeVisibleAndCheckCapability(e, false)
	}
}