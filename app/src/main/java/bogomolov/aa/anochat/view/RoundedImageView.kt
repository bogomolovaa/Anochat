package bogomolov.aa.anochat.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.BindingAdapter
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.android.getFilesDir
import java.io.File


class RoundedImageView(context: Context, attrs: AttributeSet) :
    FrameLayout(context, attrs) {

    init {
        ConstraintLayout.inflate(context, R.layout.rounded_image_layout, this)

        val a = context.obtainStyledAttributes(attrs, R.styleable.RoundedImageView, 0, 0)

        val tintColor = a.getColor(R.styleable.RoundedImageView_foregroundColor, Color.BLACK)
        val roundFg: ImageView = findViewById(R.id.round_fg_image)
        roundFg.setColorFilter(tintColor)

        val drawableResId = a.getResourceId(R.styleable.RoundedImageView_srcDrawable, -1);
        val imageView: ImageView = findViewById(R.id.round_image)
        if (drawableResId != -1) {
            val drawable = AppCompatResources.getDrawable(context, drawableResId)
            imageView.setImageDrawable(drawable)
        }
        val fileName = a.getString(R.styleable.RoundedImageView_srcFile)
        if (fileName != null) setFile(fileName)

        a.recycle()
    }

    fun setFile(fileName: String) {
        val imageView: ImageView = findViewById(R.id.round_image)
        val file = File(getFilesDir(context), fileName)
        imageView.setImageBitmap(BitmapFactory.decodeFile(file.path))
    }

}

@BindingAdapter("app:srcFile")
fun setFileName(view: RoundedImageView, fileName: String?) {
    if (!fileName.isNullOrEmpty()) {
        view.setFile(fileName)
        view.requestLayout()
    }
}