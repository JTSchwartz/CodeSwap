package com.jtschwartz.codeswap

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange

lateinit var anchorRange: TextRange

private fun getSelectionRange(editor: Editor, isSoftSelection: Boolean): TextRange {
	if (isSoftSelection) return TextRange(editor.selectionModel.selectionStart, editor.selectionModel.selectionEnd)
	
	val doc = editor.document
	
	return TextRange(doc.getLineStartOffset(doc.getLineNumber(editor.selectionModel.selectionStart)), doc.getLineEndOffset(doc.getLineNumber(editor.selectionModel.selectionEnd)))
}

private fun makeVisibleAndCheckCapability(e: AnActionEvent, needsSelection: Boolean) {
	val project = e.project
	val editor = e.getData(CommonDataKeys.EDITOR)
	
	e.presentation.isVisible = true
	e.presentation.isEnabled = project != null && editor != null
			&& editor.caretModel.caretCount == 1
			&& if (needsSelection) editor.selectionModel.hasSelection() else true
}

open class HardCodeAnchor: AnAction() {
	
	override fun actionPerformed(e: AnActionEvent) {
		anchorRange = getSelectionRange(e.getData(CommonDataKeys.EDITOR) ?: return, this is SoftCodeAnchor)
	}
	
	override fun update(e: AnActionEvent) {
		makeVisibleAndCheckCapability(e, this is SoftCodeAnchor)
	}
}

open class HardCodeSwap: AnAction() {
	lateinit var swapRange: TextRange
	
	override fun actionPerformed(e: AnActionEvent) {
		performSwap(e)
	}
	
	private fun doSelectionsOverlap(): Boolean {
		if (isAnchorFirst()) return anchorRange.endOffset > swapRange.startOffset && anchorRange.endOffset > swapRange.endOffset
		return anchorRange.endOffset < swapRange.startOffset && anchorRange.endOffset < swapRange.endOffset
	}
	
	private fun isAnchorFirst(): Boolean {
		return anchorRange.startOffset < swapRange.startOffset
	}
	
	private fun performSwap(e: AnActionEvent) {
		val editor = e.getData(CommonDataKeys.EDITOR) ?: return
		this.swapRange = getSelectionRange(editor, this is SoftCodeSwap)
		
		if (doSelectionsOverlap()) return
		
		val doc = editor.document
		val project = e.getRequiredData(CommonDataKeys.PROJECT)
		
		if (anchorRange.endOffset >= doc.textLength) anchorRange = TextRange(anchorRange.startOffset, doc.textLength)
		
		val anchorText = doc.getText(anchorRange)
		val swapText = doc.getText(swapRange)
		
		WriteCommandAction.runWriteCommandAction(project) {
			anchorRange = if (isAnchorFirst()) {
				doc.replaceString(swapRange.startOffset, swapRange.endOffset, anchorText)
				doc.replaceString(anchorRange.startOffset, anchorRange.endOffset, swapText)
				TextRange(swapRange.startOffset + (swapText.length - anchorText.length), swapRange.startOffset + swapText.length)
			} else {
				doc.replaceString(anchorRange.startOffset, anchorRange.endOffset, swapText)
				doc.replaceString(swapRange.startOffset, swapRange.endOffset, anchorText)
				TextRange(swapRange.startOffset, swapRange.startOffset + anchorText.length)
			}
		}
		
		editor.caretModel.primaryCaret.removeSelection()
	}
	
	override fun update(e: AnActionEvent) {
		makeVisibleAndCheckCapability(e, this is SoftCodeSwap)
	}
}

class SoftCodeAnchor: HardCodeAnchor()
class SoftCodeSwap: HardCodeSwap()