// NoteVault-iOS/Core/Notifications/NotificationManager.swift

import Foundation
import UserNotifications

public final class NotificationManager: @unchecked Sendable {
    public static let shared = NotificationManager()
    private let center = UNUserNotificationCenter.current()
    
    private init() {}
    
    public func requestAuthorization() async -> Bool {
        do {
            let granted = try await center.requestAuthorization(options: [.alert, .sound, .badge])
            return granted
        } catch {
            return false
        }
    }
    
    public func scheduleTaskReminder(task: TaskItem) async {
        guard let reminderDate = task.reminderDate, reminderDate > Date() else {
            return
        }
        
        let content = UNMutableNotificationContent()
        content.title = "Task Reminder"
        content.body = task.title
        content.sound = .default
        content.userInfo = ["taskId": task.id.uuidString]
        
        let calendar = Calendar.current
        let components = calendar.dateComponents([.year, .month, .day, .hour, .minute], from: reminderDate)
        let trigger = UNCalendarNotificationTrigger(dateMatching: components, repeats: false)
        
        let identifier = "task_reminder_\(task.id.uuidString)"
        let request = UNNotificationRequest(identifier: identifier, content: content, trigger: trigger)
        
        do {
            try await center.add(request)
        } catch {
            print("Failed to schedule notification: \(error)")
        }
    }
    
    public func cancelTaskReminder(taskId: UUID) {
        let identifier = "task_reminder_\(taskId.uuidString)"
        center.removePendingNotificationRequests(withIdentifiers: [identifier])
    }
}
