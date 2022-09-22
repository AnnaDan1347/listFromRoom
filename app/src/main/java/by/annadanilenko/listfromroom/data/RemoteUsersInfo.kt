package by.annadanilenko.listfromroom.data

import android.util.Log
import by.annadanilenko.listfromroom.data.api.NetClientAPI
import by.annadanilenko.listfromroom.data.ext.toRemoteGetUsersInfo
import by.annadanilenko.listfromroom.data.model.User
import by.annadanilenko.listfromroom.data.model.UsersResponse
import by.annadanilenko.listfromroom.data.model.dbroom.AppDatabase
import by.annadanilenko.listfromroom.data.model.dbroom.ItemUser
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class RemoteUsersInfo @Inject constructor(
    private val netClientAPI: NetClientAPI,
    private val appDatabase: AppDatabase
) {
    suspend fun getUsersInfo(): Boolean {

        withContext(Dispatchers.IO) {
            appDatabase.itemDao!!.deleteAllUsers()
        }

        var usersResponse: UsersResponse? = null
        val res = netClientAPI.getUsersInfo()
        if (res.code == 200) {
            usersResponse = Gson().toRemoteGetUsersInfo(res.body.toString())
            try {
                usersResponse?.users?.let { saveUsersToDB(it) }
            } catch (e: Exception) {
                Log.i("TESTER_DB", "ОШИБКА ЗАПИСИ")
                Log.i("TESTER_DB", e.message.toString())
                return false
            }
        } else {
            usersResponse?.errorText = "Что-то пошло не так!"

        }
        usersResponse?.serverCode = res.code

        return true
    }

    private suspend fun saveUsersToDB(
        jsonItems: List<User>
    ) {
        for (el in jsonItems) {
            val item: ItemUser = (Gson().fromJson<Any>(
                el.toString(),
                ItemUser::class.java
            )) as ItemUser

            withContext(Dispatchers.IO) {
                val itemUser = ItemUser(
                    id = item.id,
                    userName = item.userName,
                    imageUrl = item.imageUrl,
                    originalApi = item.originalApi
                )

                appDatabase.itemDao!!.insert(itemUser)

            }
        }
    }
}