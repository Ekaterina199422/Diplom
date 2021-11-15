package ru.netology.diplom.utils

import android.app.Activity
import android.app.AlertDialog
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import ru.netology.diplom.R

class PremissionsManager(val activity: Activity, val list: List<String>, val code: Int) {

fun checkPermissions(): Boolean {
    return isPermissionsGranted() == PackageManager.PERMISSION_GRANTED
}



private fun isPermissionsGranted(): Int {

    var counter = 0
    for (permission in list) {
        counter += ContextCompat.checkSelfPermission(activity, permission)
    }
    return counter
}


private fun deniedPermission(): String {
    for (permission in list) {
        if (ContextCompat.checkSelfPermission(activity, permission)
            == PackageManager.PERMISSION_DENIED
        ) return permission
    }
    return ""
}



private fun showAlert(permission: String) {
    val builder = androidx.appcompat.app.AlertDialog.Builder(activity)
    builder.setTitle(activity.getString(R.string.permissions_needed_title))
    builder.setMessage(activity.getString(R.string.grant_following_permissions_dialog_message))
    builder.setPositiveButton(activity.getString(R.string.ok_action)) { dialog, which ->
        ActivityCompat.requestPermissions(activity, arrayOf(permission), code)

    }
    builder.setNeutralButton(activity.getString(R.string.action_cancel), null)
    val dialog = builder.create()
    dialog.show()
}



fun requestPermissions() {
    val permission = deniedPermission()
    if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
        showAlert(permission)
    } else {
        ActivityCompat.requestPermissions(activity, list.toTypedArray(), code)
    }
}
}