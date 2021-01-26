package name.guai.macrophage;

import com.google.common.collect.Iterables;
import com.intellij.ide.actionMacro.ActionMacro;
import com.intellij.ide.actionMacro.ActionMacroManager;
import com.intellij.idea.IdeaLogger;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ActionManagerEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.usages.UsageInfo2UsageAdapter;
import com.intellij.usages.UsageView;
import com.intellij.usages.UsageViewManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MacrophageAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull final AnActionEvent anActionEvent) {

        Project project = anActionEvent.getProject();
        if (project == null) {
            StatusBar.Info.set("Project is null", null);
            IdeaLogger.getInstance(getClass()).error("project is null");
            return;
        }

        UsageView selectedUsageView = UsageViewManager.getInstance(project).getSelectedUsageView();
        if (selectedUsageView == null) {
            StatusBar.Info.set("No usages view", project);
            return;
        }

        ListPopup popup = JBPopupFactory.getInstance().createActionGroupPopup("Select Macro for Macrophage", new MyMacrosGroup(selectedUsageView), anActionEvent.getDataContext(), JBPopupFactory.ActionSelectionAid.SPEEDSEARCH, false);
        popup.showCenteredInCurrentWindow(project);
    }

    static class MyMacrosGroup extends ActionGroup {
        private final UsageView usageView;

        MyMacrosGroup(UsageView usageView) {
            this.usageView = usageView;
        }

        @NotNull
        public AnAction[] getChildren(@Nullable AnActionEvent e) {

            ArrayList<AnAction> actions = new ArrayList<>();
            ActionManagerEx actionManager = ((ActionManagerEx) ActionManager.getInstance());
            List<String> ids = actionManager.getActionIdList(ActionMacro.MACRO_ACTION_PREFIX);

            for (String id : ids) {
                String name = id.substring(ActionMacro.MACRO_ACTION_PREFIX.length());
                actions.add(new AnAction(name) {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e) {
                        ActionMacroManager macroManager = ActionMacroManager.getInstance();

                        ActionMacro targetMacro = Arrays.stream(macroManager.getAllMacros()).filter(it -> it.getName().equals(name)).findFirst().orElse(null);
                        if (targetMacro == null) return;

                        int size = Iterables.size(Iterables.filter(usageView.getUsages(), UsageInfo2UsageAdapter.class));

                        UberMacro uberMacroGutter = new UberMacro();
                        for (int i = 0; i < size; i++) {
                            uberMacroGutter.appendActionDescriptor(new ActionMacro.IdActionDescriptor("name.guai.macrophage.GotoNextUsageAction"));
                            for (ActionMacro.ActionDescriptor actionDescriptor : targetMacro.getActions())
                                uberMacroGutter.appendActionDescriptor(new DelegateActionDescriptor(actionDescriptor));
                        }
                        macroManager.playMacro(uberMacroGutter.macro);
                    }
                });
            }

            return actions.toArray(new AnAction[actions.size()]);
        }
    }

    private static class UberMacro {
        private final ActionMacro macro = new ActionMacro();
        private final ArrayList<ActionMacro.ActionDescriptor> myActions = reflectGuts();

        private ArrayList<ActionMacro.ActionDescriptor> reflectGuts() {
            Field myActions;
            try {
                myActions = ActionMacro.class.getDeclaredField("myActions");
            } catch (NoSuchFieldException e) {
                try {
                    myActions = ActionMacro.class.getDeclaredField("b"); //when obfuscated
                } catch (NoSuchFieldException noSuchFieldException) {
                    throw new RuntimeException(noSuchFieldException);
                }
            }
            myActions.setAccessible(true);
            try {
                //noinspection unchecked
                return (ArrayList<ActionMacro.ActionDescriptor>) myActions.get(macro);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        void appendActionDescriptor(ActionMacro.ActionDescriptor actionDescriptor) {
            myActions.add(actionDescriptor);
        }
	}

    private static class DelegateActionDescriptor implements ActionMacro.ActionDescriptor {
        private final ActionMacro.ActionDescriptor delegate;

        private DelegateActionDescriptor(ActionMacro.ActionDescriptor delegate) {
            this.delegate = delegate;
        }

        public Object clone() {
            return new DelegateActionDescriptor((ActionMacro.ActionDescriptor) delegate.clone());
        }

        public void playBack(DataContext context) {
            delegate.playBack(context);
        }

        public void generateTo(StringBuffer script) {
            delegate.generateTo(script);
        }
    }
}
