// NoteVault-iOS/Views/Navigation/MainNavigationView.swift

import SwiftUI

public enum NavigationTab: Hashable {
    case notes
    case logicBoard
    case tasks
    case search
    case settings
}

public struct MainNavigationView: View {
    @State private var selectedTab: NavigationTab = .notes
    
    public init() {}
    
    public var body: some View {
        TabView(selection: $selectedTab) {
            NotesListView()
                .tabItem {
                    Label("Notes", systemImage: "note.text")
                }
                .tag(NavigationTab.notes)
            
            LogicBoardCanvasView()
                .tabItem {
                    Label("Logic Board", systemImage: "point.3.filled.connected.trianglepath.dotted")
                }
                .tag(NavigationTab.logicBoard)
            
            TasksListView()
                .tabItem {
                    Label("Tasks", systemImage: "checklist")
                }
                .tag(NavigationTab.tasks)
            
            GlobalSearchView()
                .tabItem {
                    Label("Search", systemImage: "magnifyingglass")
                }
                .tag(NavigationTab.search)
            
            SettingsView()
                .tabItem {
                    Label("Settings", systemImage: "gearshape")
                }
                .tag(NavigationTab.settings)
        }
    }
}
