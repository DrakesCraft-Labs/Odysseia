package org.metamechanists.odysseia.boss.skills;

import org.bukkit.entity.Player;
import org.metamechanists.odysseia.boss.OdysseyBoss;

public interface BossSkill {
    void execute(OdysseyBoss boss, Player target);
}
