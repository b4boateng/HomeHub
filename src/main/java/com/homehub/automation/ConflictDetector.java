package com.homehub.automation;

import java.util.List;

import com.homehub.exceptions.RuleConflictException;

public class ConflictDetector {

    public static void check(Rule newRule, List<Rule> existing) {
        for (Rule other : existing) {
            //Conflicts occur when a device has two rules with the same priority but different actions
            boolean sameDevice = other.getTargetDeviceId().equals(newRule.getTargetDeviceId());
            boolean samePriority = other.getPriority() == newRule.getPriority();
            boolean differentAction = other.getCommand().action() != newRule.getCommand().action();

            if (sameDevice && samePriority && differentAction) {
                throw new RuleConflictException("Rules for '" + newRule.getTargetDeviceId()
                        + "' both have priority " + newRule.getPriority() + " but ask for "
                        + other.getCommand().action() + " and " + newRule.getCommand().action());
            }
        }
    }
}
