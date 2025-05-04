package com.plugin.gitmultimerge.util;

import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * Utilitário de notificações simplificado para exibir mensagens ao usuário.
 */
public class NotificationHelper {

    /**
     * Título padrão para notificações do plugin.
     */
    public static final String DEFAULT_TITLE = "Git Multi Merge";

    /**
     * Grupo de notificações para o plugin.
     */
    private static final NotificationGroup NOTIFICATION_GROUP = NotificationGroupManager.getInstance()
            .getNotificationGroup("Git Multi Merge");

    /**
     * Exibe notificação de erro.
     *
     * @param project Projeto atual
     * @param title   Título da notificação
     * @param message Mensagem de erro
     */
    public static void notifyError(@NotNull Project project, @NotNull String title, @NotNull String message) {
        // Usando a API atualizada para 2025
        NOTIFICATION_GROUP.createNotification(message, NotificationType.ERROR)
                .setTitle(title)
                .notify(project);
    }

    /**
     * Exibe notificação de erro a partir de uma exceção.
     *
     * @param project Projeto atual
     * @param title   Título da notificação
     * @param error   Exceção ocorrida
     */
    public static void notifyError(@NotNull Project project, @NotNull String title, @NotNull Throwable error) {
        String errorMessage = error.getMessage();
        if (errorMessage == null || errorMessage.isEmpty()) {
            errorMessage = error.getClass().getSimpleName();
        }
        notifyError(project, title, errorMessage);
    }

    /**
     * Exibe notificação de sucesso.
     *
     * @param project Projeto atual
     * @param title   Título da notificação
     * @param message Mensagem de sucesso
     */
    public static void notifySuccess(@NotNull Project project, @NotNull String title, @NotNull String message) {
        // Usando a API atualizada para 2025
        NOTIFICATION_GROUP.createNotification(message, NotificationType.INFORMATION)
                .setTitle(title)
                .notify(project);
    }

    /**
     * Exibe notificação de aviso.
     *
     * @param project Projeto atual
     * @param title   Título da notificação
     * @param message Mensagem de aviso
     */
    public static void notifyWarning(@NotNull Project project, @NotNull String title, @NotNull String message) {
        // Usando a API atualizada para 2025
        NOTIFICATION_GROUP.createNotification(message, NotificationType.WARNING)
                .setTitle(title)
                .notify(project);
    }
}