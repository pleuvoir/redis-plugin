@echo on
@echo =============================================================
@echo $                                                           $
@echo $                      redis-plugin                         $
@echo $                                                           $
@echo =============================================================
@echo.
@echo off

@title redis-plugin deploy
@color 0a

rem  Please execute command in local directory.

call mvn clean deploy -DskipTests -P release

pause