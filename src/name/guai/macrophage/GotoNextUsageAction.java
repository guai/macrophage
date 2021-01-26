package name.guai.macrophage;

import com.google.common.collect.Iterables;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.usages.Usage;
import com.intellij.usages.UsageInfo2UsageAdapter;
import com.intellij.usages.UsageView;
import com.intellij.usages.UsageViewManager;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

public class GotoNextUsageAction extends AnAction {

	private static UsageView previousUsageView = null;
	private static Iterator<UsageInfo2UsageAdapter> iterator = null;

	@Override
	public void actionPerformed(@NotNull final AnActionEvent anActionEvent) {

		Project project = anActionEvent.getProject();
		if(project == null) {
			StatusBar.Info.set("Project is null", null);
			return;
		}

		UsageView selectedUsageView = UsageViewManager.getInstance(project).getSelectedUsageView();
		if(selectedUsageView == null) {
			StatusBar.Info.set("No usages view", project);
			return;
		}

		if(selectedUsageView != previousUsageView) {
			iterator = Iterables.filter(selectedUsageView.getSortedUsages(), UsageInfo2UsageAdapter.class).iterator();
			previousUsageView = selectedUsageView;
		}

		while(true) {
			if(!iterator.hasNext())
				throw new RuntimeException("No more usages");
			Usage usage = iterator.next();
			if(usage.isValid()) {
				usage.navigate(true);
				usage.selectInEditor();
				return;
			}
		}
	}
}
