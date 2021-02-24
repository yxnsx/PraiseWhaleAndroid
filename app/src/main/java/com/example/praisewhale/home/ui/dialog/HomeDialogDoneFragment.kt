package com.example.praisewhale.home.ui.dialog

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.praisewhale.CollectionImpl
import com.example.praisewhale.MainActivity
import com.example.praisewhale.R
import com.example.praisewhale.databinding.DialogHomeDoneBinding
import com.example.praisewhale.home.adapter.RecentPraiseToAdapter
import com.example.praisewhale.home.data.RequestPraiseDone
import com.example.praisewhale.home.data.ResponseDonePraise
import com.example.praisewhale.home.data.ResponseRecentPraiseTo
import com.example.praisewhale.home.ui.HomeFragment
import com.example.praisewhale.util.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class HomeDialogDoneFragment : DialogFragment(), RecentPraiseToClickListener {

    private var _viewBinding: DialogHomeDoneBinding? = null
    private val viewBinding get() = _viewBinding!!

    private var praiseId: Int = 0
    private val sharedPreferences = MyApplication.mySharedPreferences


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _viewBinding = DialogHomeDoneBinding.inflate(layoutInflater)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setListeners()
        setDialogBackground()
        getServerRecentPraiseTo()
    }

    private fun setListeners() {
        viewBinding.apply {
            imageButtonClose.setOnClickListener(fragmentClickListener)
            editTextPraiseTo.addTextChangedListener(dialogTextWatcher)
            imageButtonDelete.setOnClickListener(fragmentClickListener)
            recyclerViewRecentPraiseTo.addOnScrollListener(dialogDoneScrollListener)
            buttonConfirm.setOnClickListener(fragmentClickListener)
        }
    }

    private fun setDialogBackground() {
        dialog!!.window!!.setBackgroundDrawableResource(R.drawable.background_rectangle_radius_15_stroke)
    }

    private fun getServerRecentPraiseTo() {
        val call: Call<ResponseRecentPraiseTo> = CollectionImpl.service.getRecentPraiseTo(
            sharedPreferences.getValue("token", "")
        )
        call.enqueue(object : Callback<ResponseRecentPraiseTo> {
            override fun onFailure(call: Call<ResponseRecentPraiseTo>, t: Throwable) {
                Log.d("tag", t.localizedMessage!!)
            }

            override fun onResponse(
                call: Call<ResponseRecentPraiseTo>,
                response: Response<ResponseRecentPraiseTo>
            ) {
                when (response.isSuccessful) {
                    true -> setRecentPraiseToView(response.body()!!.data)
                    false -> handleRecentPraiseToStatusCode(response)
                }
            }
        })
    }

    private fun setRecentPraiseToView(recentPraiseToList: List<ResponseRecentPraiseTo.Name>) {
        val testList = listOf(
            ResponseRecentPraiseTo.Name("김송현"),
            ResponseRecentPraiseTo.Name("남궁선규"),
            ResponseRecentPraiseTo.Name("최윤소")
        )
        viewBinding.apply {
            recyclerViewRecentPraiseTo.adapter = RecentPraiseToAdapter(testList, this@HomeDialogDoneFragment)
            when (testList.size) {
                0 -> textViewRecentPraiseToTitle.setInvisible()
                else -> textViewRecentPraiseToTitle.setVisible()
            }
        }
    }

    private fun handleRecentPraiseToStatusCode(response: Response<ResponseRecentPraiseTo>) {
        when (response.code()) {
//            401 -> getServerRecentPraiseTo() todo - 토큰 값 갱신 후 재요청
            else -> Log.d("TAG", "handleStatusCode: ${response.code()}, ${response.message()}")
        }
    }

    private fun saveServerPraiseData(target: String) {
        val call: Call<ResponseDonePraise> = CollectionImpl.service.postPraiseDone(
            sharedPreferences.getValue("token", ""),
            praiseId,
            RequestPraiseDone(target)
        )
        call.enqueue(object : Callback<ResponseDonePraise> {
            override fun onFailure(call: Call<ResponseDonePraise>, t: Throwable) {
                Log.d("tag", t.localizedMessage!!)
            }

            override fun onResponse(
                call: Call<ResponseDonePraise>,
                response: Response<ResponseDonePraise>
            ) {
                when (response.isSuccessful) {
                    true -> {
                        showResultDialog(response.body()!!.data.isLevelUp)
                        dialog!!.dismiss()
                    }
                    false -> handleSaveServerPraiseStatusCode(response, target)
                }
            }
        })
    }

    private fun handleSaveServerPraiseStatusCode(
        response: Response<ResponseDonePraise>,
        target: String
    ) {
        when (response.code()) {
//            401 -> saveServerPraiseData(target) todo - 토큰 값 갱신 후 재요청
            else -> { // todo - 각 에러 코드별 처리..
                Log.d("TAG", "handleStatusCode: ${response.code()}, ${response.message()}")
            }
        }
    }

    private fun showResultDialog(isLevelUp: Boolean) {
        val dialogDoneResult = HomeDialogDoneResultFragment.CustomDialogBuilder()
            .getLevelUpStatus(isLevelUp)
            .create()
        dialogDoneResult.show(parentFragmentManager, dialogDoneResult.tag)
        sharedPreferences.setValue(LAST_PRAISE_STATUS, "done")
    }

    private val dialogDoneScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            (recyclerView.layoutManager as LinearLayoutManager).apply {
                updateBlurBox(
                    findFirstCompletelyVisibleItemPosition(),
                    findLastCompletelyVisibleItemPosition()
                )
            }
        }
    }

    private fun updateBlurBox(
        firstCompletelyVisibleItemPosition: Int,
        lastCompletelyVisibleItemPosition: Int
    ) {
        viewBinding.apply {
            if (firstCompletelyVisibleItemPosition == 0) {
                imageViewBlurBoxLeft.fadeOut()
                imageViewBlurBoxRight.fadeIn()
            }
            if (lastCompletelyVisibleItemPosition == 2) {
                imageViewBlurBoxLeft.fadeIn()
                imageViewBlurBoxRight.fadeOut()
            } else {
                imageViewBlurBoxLeft.fadeIn()
                imageViewBlurBoxRight.fadeIn()
            }
        }
    }

    private val fragmentClickListener = View.OnClickListener {
        viewBinding.apply {
            when (it.id) {
                imageButtonClose.id -> dialog!!.dismiss()
                imageButtonDelete.id -> editTextPraiseTo.setText("")
                buttonConfirm.id -> {
                    saveServerPraiseData(editTextPraiseTo.text.toString())
                    showResultDialog(true)
                    dialog!!.dismiss()
                    (activity as MainActivity).changeFragment(HomeFragment())
                }
            }
        }
    }

    private val dialogTextWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {}
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            updateDeleteButtonVisibility()
        }
    }

    private fun updateDeleteButtonVisibility() {
        viewBinding.apply {
            when (editTextPraiseTo.text.toString()) {
                "" -> imageButtonDelete.setInvisible()
                else -> imageButtonDelete.setVisible()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _viewBinding = null
    }

    override fun onClickRecentPraiseToItem(recentPraiseTo: String) {
        viewBinding.editTextPraiseTo.setText(recentPraiseTo)
    }


    class CustomDialogBuilder {
        private val dialog = HomeDialogDoneFragment()

        fun getPraiseIndex(praiseId: Int): CustomDialogBuilder {
            dialog.praiseId = praiseId
            return this
        }

        fun create(): HomeDialogDoneFragment {
            return dialog
        }
    }
}