package name.guai.macrophage

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.wm.StatusBar
import com.intellij.usages.UsageInfo2UsageAdapter
import com.intellij.usages.UsageView
import com.intellij.usages.UsageViewManager

class GotoNextUsageAction : AnAction() {
    companion object {
        private var previousUsageView: UsageView? = null
        private lateinit var iterator: Iterator<UsageInfo2UsageAdapter>
    }

    override fun actionPerformed(anActionEvent: AnActionEvent) {
        val project = anActionEvent.project ?: run {
            StatusBar.Info.set("Project is null", null)
            return
        }
        val selectedUsageView = UsageViewManager.getInstance(project).selectedUsageView ?: run {
            StatusBar.Info.set("No usages view", project)
            return
        }
        if (selectedUsageView !== previousUsageView) {
            iterator = selectedUsageView.sortedUsages.filterIsInstance<UsageInfo2UsageAdapter>().iterator()
            previousUsageView = selectedUsageView
        }
        while (true) {
            if (!iterator.hasNext()) throw RuntimeException("No more usages")
            val usage = iterator.next()
            if (usage.isValid) {
                usage.navigate(true)
                usage.selectInEditor()
                return
            }
        }
    }
}
