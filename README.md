## VillagerRNGDebugMod for 1.17.1

Target level after upgrade (1~4, level 5 has no enchanted book): 2

Tradeable enchantments:
0. protection                Lv1~4
1. fire_protection           Lv1~4
2. feather_falling           Lv1~4
3. blast_protection          Lv1~4
4. projectile_protection     Lv1~4
5. respiration               Lv1~3
6. aqua_affinity             Lv1
7. thorns                    Lv1~3
8. depth_strider             Lv1~3
9. frost_walker              Lv1~2
10. binding_curse             Lv1
11. soul_speed                Lv1~3
12. sharpness                 Lv1~5
13. smite                     Lv1~5
14. bane_of_arthropods        Lv1~5
15. knockback                 Lv1~2
16. fire_aspect               Lv1~2
17. looting                   Lv1~3
18. sweeping                  Lv1~3
19. efficiency                Lv1~5
20. silk_touch                Lv1
21. unbreaking                Lv1~3
22. fortune                   Lv1~3
23. power                     Lv1~5
24. punch                     Lv1~2
25. flame                     Lv1
26. infinity                  Lv1
27. luck_of_the_sea           Lv1~3
28. lure                      Lv1~3
29. loyalty                   Lv1~3
30. impaling                  Lv1~5
31. riptide                   Lv1~3
32. channeling                Lv1
33. multishot                 Lv1
34. quick_charge              Lv1~3
35. piercing                  Lv1~4
36. mending                   Lv1
37. vanishing_curse           Lv1

Target enchantment (e.g. mending / efficiency): mending
Max price in emeralds (Enter = no limit): 10
Extra LCG steps before AMBIENT (nextInt/Bool/Float=1, nextDouble=2, Enter=0):

Searching... target: mending  price<=10
Villager level: 2,  search range: seed 0 ~ 16777216

Found 5 matching seed(s):

set-seed 0x00000000021C  (540)
-> trades after upgrade: MENDING 1  (10 emeralds) | (fixed trade)

set-seed 0x0000000006EA  (1770)
-> trades after upgrade: MENDING 1  (10 emeralds) | (fixed trade)

set-seed 0x000000000A1E  (2590)
-> trades after upgrade: MENDING 1  (10 emeralds) | (fixed trade)

set-seed 0x000000000F94  (3988)
-> trades after upgrade: MENDING 1  (10 emeralds) | (fixed trade)

set-seed 0x000000000FB1  (4017)
-> trades after upgrade: MENDING 1  (10 emeralds) | (fixed trade)

Steps:
1. Complete the last trade (XP fills up), updateMerchantTimer=40
2. Wait 39 ticks (updateMerchantTimer=1)
3. Run /vrng set-seed <seed above>
4. Next tick: villager levels up with the target trade





------


[![License](https://img.shields.io/github/license/Fallen-Breath/fabric-mod-template.svg)](http://www.gnu.org/licenses/lgpl-3.0.html)
[![workflow](https://github.com/Fallen-Breath/fabric-mod-template/actions/workflows/gradle.yml/badge.svg)](https://github.com/Fallen-Breath/fabric-mod-template/actions/workflows/gradle.yml)

fallen's fabric mod template

If you find it helpful, a credit to this template in your project will be greatly appreciated

## To use

1. Clone / Use this template to get a new project
2. Search `[FEATURE]` in the project, delete or uncomment those addons
3. Setup the mod
    - Edit java package name
    - Edit [gradle.properties](gradle.properties) for mod id / name etc.
    - Edit mod name in [bug_report.yml](.github/ISSUE_TEMPLATE/bug_report.yml)
    - Edit [common.gradle](common.gradle) for mod file location constants
    - Change the Minecraft versions in [settings.json](settings.json), [build.gradle](build.gradle), and files in the [versions](versions) folder
    - Search `template` in the project to see if there are any missing unedited stuffs
4. Edit [README](README.md) for the new mod
