package com.homehub.automation;

import com.homehub.exceptions.*;

import java.util.List;

public class ConflictDetector {

    public static void check(Rule newRule, List<Rule> existing) {
        for (Rule other : existing) {
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
