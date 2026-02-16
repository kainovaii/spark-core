package fr.kainovaii.obsidian.security.role;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface HasRole
{
    String value();
}