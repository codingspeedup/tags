package io.github.codingspeedup.tags.actions;

import com.intellij.ide.IdeEventQueue;
import com.intellij.openapi.actionSystem.ActionPromoter;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DataContext;
import org.jetbrains.annotations.NotNull;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

public class ChatActionPromoter implements ActionPromoter {

    @Override
    public List<AnAction> promote(@NotNull List<? extends AnAction> actions, @NotNull DataContext context) {
        var event = IdeEventQueue.getInstance().getTrueCurrentEvent();
        if (event instanceof KeyEvent keyEvent && keyEvent.getExtendedKeyCode() == KeyEvent.VK_ENTER) {
            int modifiers = keyEvent.getModifiersEx();
            boolean isModifierDown = (modifiers & InputEvent.CTRL_DOWN_MASK) != 0
                    || (modifiers & InputEvent.META_DOWN_MASK) != 0;
            if (isModifierDown) {
                for (AnAction action : actions) {
                    if (action instanceof ExecutePromptAction) {
                        return List.of(action);
                    }
                }
            }
        }
        return new ArrayList<>(actions);
    }

}