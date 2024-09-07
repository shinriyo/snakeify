import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.rename.RenameProcessor
import com.intellij.util.IncorrectOperationException

class RenameToSnakeCaseAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val file = event.getData(CommonDataKeys.VIRTUAL_FILE) ?: return

        if (file.name.endsWith(".dart")) {
            val psiFile = PsiManager.getInstance(project).findFile(file) ?: run {
                Messages.showErrorDialog(project, "ファイルを読み込めませんでした。", "エラー")
                return
            }

            // ファイルからクラス名を抽出する
            val classes = extractClasses(psiFile)

            if (classes.isEmpty()) {
                Messages.showInfoMessage(project, "ファイル内にクラスが見つかりませんでした。", "情報")
                return
            }

            // 複数のクラスがある場合、選択ダイアログを表示
            val selectedClass = if (classes.size == 1) classes[0] else chooseClassFromList(classes, project) ?: return

            val originalName = selectedClass.name ?: return
            val snakeCaseName = toSnakeCase(originalName)

            // リファクタリングの確認ダイアログ
            val result = Messages.showYesNoDialog(
                project,
                "$snakeCaseName にリネームしてリファクタリングを行いますか？",
                "リネームとリファクタリングの確認",
                Messages.getQuestionIcon()
            )

            if (result == Messages.YES) {
                // リファクタリングを実行
                try {
                    RenameProcessor(project, selectedClass, snakeCaseName, false, false).run()
                } catch (e: IncorrectOperationException) {
                    Messages.showErrorDialog(
                        project,
                        "リファクタリング中にエラーが発生しました: ${e.message}",
                        "エラー"
                    )
                }
            }
        }
    }

    private fun toSnakeCase(input: String): String {
        return input.replace(Regex("([a-z])([A-Z])"), "$1_$2").lowercase()
    }

    private fun extractClasses(psiFile: PsiFile): List<PsiClass> {
        // PsiFileからクラスを抽出
        return PsiTreeUtil.findChildrenOfType(psiFile, PsiClass::class.java).toList()
    }

    private fun chooseClassFromList(classes: List<PsiClass>, project: Project): PsiClass? {
        val classNames = classes.mapNotNull { it.name }.toTypedArray()
        val selected = Messages.showEditableChooseDialog(
            "リネームするクラスを選択してください:",
            "クラス選択",
            null,
            classNames,
            classNames.firstOrNull(),
            null
        )

        return classes.firstOrNull { it.name == selected }
    }
}
