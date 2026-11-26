package com.multiclone.app

import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        // Clone space (work profile) wali copy mein zaroori cheezein enable karo
        ProfileSetup.prepareIfProfileOwner(this)
    }
}
