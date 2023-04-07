package name.guai.macrophage

import com.intellij.ide.actionMacro.ActionMacro
import com.intellij.ide.actionMacro.ActionMacro.ActionDescriptor
import com.intellij.ide.actionMacro.ActionMacro.IdActionDescriptor
import com.intellij.ide.actionMacro.ActionMacroManager
import com.intellij.idea.IdeaLogger
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ex.ActionManagerEx
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.StatusBar
import com.intellij.usages.UsageInfo2UsageAdapter
import com.intellij.usages.UsageView
import com.intellij.usages.UsageViewManager

class MacrophageAction : AnAction() {
    override fun actionPerformed(anActionEvent: AnActionEvent) {
        val project = anActionEvent.project ?: run {
            StatusBar.Info.set("Project is null", null)
            IdeaLogger.getInstance(javaClass).error("project is null")
            return
        }
        val selectedUsageView = UsageViewManager.getInstance(project).selectedUsageView ?: run {
            StatusBar.Info.set("No usages view", project)
            return
        }
        val popup = JBPopupFactory.getInstance().createActionGroupPopup(
            "Select Macro for Macrophage",
            MyMacrosGroup(selectedUsageView),
            anActionEvent.dataContext,
            JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
            false
        )
        popup.showCenteredInCurrentWindow(project)
    }

    internal class MyMacrosGroup(private val usageView: UsageView) : ActionGroup() {
        override fun getChildren(e: AnActionEvent?): Array<AnAction> {
            val actions = mutableListOf<AnAction>()
            val actionManager = ActionManager.getInstance() as ActionManagerEx
            val ids = actionManager.getActionIdList(ActionMacro.MACRO_ACTION_PREFIX)
            for (id in ids) {
                val name = id.substring(ActionMacro.MACRO_ACTION_PREFIX.length)
                actions.add(
                    object : AnAction(name) {
                        override fun actionPerformed(e: AnActionEvent) {
                            val macroManager = ActionMacroManager.getInstance()
                            val targetMacro = macroManager.allMacros.first { it!!.name == name } ?: return
                            val size = usageView.usages.filterIsInstance<UsageInfo2UsageAdapter>().count()
                            val uberMacroGutter = UberMacro()
                            for (i in 0 until size) {
                                uberMacroGutter.appendActionDescriptor(IdActionDescriptor("name.guai.macrophage.GotoNextUsageAction"))
                                for (actionDescriptor in targetMacro.actions)
                                    uberMacroGutter.appendActionDescriptor(DelegateActionDescriptor(actionDescriptor))
                            }
                            macroManager.playMacro(uberMacroGutter.macro)
                        }
                    }
                )
            }
            return actions.toTypedArray()
        }
    }

    private class UberMacro {
        val macro = ActionMacro()
        private val myActions = reflectGuts()
        private fun reflectGuts(): ArrayList<ActionDescriptor> {
            val myActions = try {
                ActionMacro::class.java.getDeclaredField("myActions")
            } catch (e: NoSuchFieldException) {
                try {
                    ActionMacro::class.java.getDeclaredField("b") //when obfuscated
                } catch (noSuchFieldException: NoSuchFieldException) {
                    throw RuntimeException(noSuchFieldException)
                }
            }.apply {
                isAccessible = true
            }
            return try {
                myActions.get(macro) as ArrayList<ActionDescriptor>
            } catch (e: IllegalAccessException) {
                throw RuntimeException(e)
            }
        }

        fun appendActionDescriptor(actionDescriptor: ActionDescriptor) {
            myActions.add(actionDescriptor)
        }
    }

    private class DelegateActionDescriptor(private val delegate: ActionDescriptor) :
        ActionDescriptor by delegate
}
