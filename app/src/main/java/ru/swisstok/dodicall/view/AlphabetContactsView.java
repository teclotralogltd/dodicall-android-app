/*
 *
 * Copyright (C) 2016, Telco Cloud Trading & Logistic Ltd
 *
 * This file is part of dodicall.
 * dodicall is free software : you can redistribute it and / or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * dodicall is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with dodicall.If not, see <http://www.gnu.org/licenses/>.
 */

package ru.swisstok.dodicall.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.Pair;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.swisstok.dodicall.R;

public class AlphabetContactsView extends View {

    private static final String DOT_CHAR = "•";
    private static final String NUMBER_CHAR = "#";
    private static final float LETTER_CELL_SIZE = 1.6f;

    private static final List<String> RUSSIAN_ALPHABET;
    private static final List<String> ENGLISH_ALPHABET;

    private int mMaxHeight;
    private int mLettersNumber;
    private String mCurrentLetter;

    private Paint mLetterPaint;
    private Paint mDividerPaint;
    private int mDotRadius;

    private List<String> mFullAlphabet;
    private List<String> mDisplayedAlphabet;
    private Map<String, Pair<Integer, Integer>> mLetterSizes;

    private AlphabetListener mAlphabetListener;

    static {
        RUSSIAN_ALPHABET = Arrays.asList("А", "Б", "В", "Г", "Д", "Е", "Ё", "Ж", "З", "И", "К", "Л", "М", "Н", "О", "П", "Р", "С", "Т", "У", "Ф", "Х", "Ц", "Ч", "Ш", "Щ", "Э", "Ю", "Я");
        ENGLISH_ALPHABET = new ArrayList<>();
        for (char c = 'A'; c <= 'Z'; c++) {
            ENGLISH_ALPHABET.add(String.valueOf(c));
        }
    }

    public AlphabetContactsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public AlphabetContactsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.AlphabetContactsView);

        mFullAlphabet = new ArrayList<>();
        mDisplayedAlphabet = new ArrayList<>();

        mDotRadius = a.getDimensionPixelSize(R.styleable.AlphabetContactsView_dotRadius, getResources().getDimensionPixelSize(R.dimen.divider_height));

        mDividerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mDividerPaint.setColor(a.getColor(R.styleable.AlphabetContactsView_android_divider, ContextCompat.getColor(context, R.color.dividerColor)));
        mDividerPaint.setStrokeWidth(a.getDimensionPixelSize(R.styleable.AlphabetContactsView_android_dividerHeight, getResources().getDimensionPixelSize(R.dimen.divider_width)));

        mLetterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mLetterPaint.setColor(a.getColor(R.styleable.AlphabetContactsView_android_textColor, ContextCompat.getColor(context, R.color.colorPrimary)));
        mLetterPaint.setTypeface(Typeface.DEFAULT);
        mLetterPaint.setTextSize(a.getDimensionPixelSize(R.styleable.AlphabetContactsView_android_textSize, getResources().getDimensionPixelSize(R.dimen.dialpad_button_text_size_number)));

        mLetterSizes = new HashMap<>();
        List<String> charsToMeasure = new ArrayList<>(RUSSIAN_ALPHABET);
        charsToMeasure.addAll(ENGLISH_ALPHABET);
        charsToMeasure.add(DOT_CHAR);
        charsToMeasure.add(NUMBER_CHAR);

        Rect rect = new Rect();
        int letterHeight = 0;
        for (String letter : charsToMeasure) {
            mLetterPaint.getTextBounds(letter, 0, 1, rect);
            mLetterSizes.put(letter, Pair.create(rect.width(), rect.height()));
            if (letterHeight < rect.height()) {
                letterHeight = rect.height();
            }
        }
        mMaxHeight = (int) (letterHeight * LETTER_CELL_SIZE);

        a.recycle();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int spaceForDraw = getMeasuredHeight() - getPaddingTop() - getPaddingBottom();
        mLettersNumber = spaceForDraw / mMaxHeight;
        setupDisplayedAlphabet();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getMeasuredWidth();
        int maxHeight = mLettersNumber < mDisplayedAlphabet.size() ? mMaxHeight : getMeasuredHeight() / mDisplayedAlphabet.size();
        int cellBottom = maxHeight;

        for (String letter : mDisplayedAlphabet) {
            if (DOT_CHAR.equals(letter)) {
                canvas.drawCircle(width / 2, cellBottom - maxHeight / 2, mDotRadius, mLetterPaint);
            } else {
                int letterX = (width - mLetterSizes.get(letter).first) / 2;
                int letterY = (maxHeight - mLetterSizes.get(letter).second) / 2;
                canvas.drawText(letter, letterX, cellBottom - letterY, mLetterPaint);
            }
            canvas.drawLine(0, cellBottom, width, cellBottom, mDividerPaint);

            cellBottom += maxHeight;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            float y = event.getY();
            checkScroll(y);
        }
        return true;
    }

    public void setupLanguage(String language) {
        mFullAlphabet.clear();
        if (language.contains(getContext().getString(R.string.pref_interface_language_ru_value))) {
            mFullAlphabet.addAll(RUSSIAN_ALPHABET);
        }
        mFullAlphabet.addAll(ENGLISH_ALPHABET);

        setupDisplayedAlphabet();
    }

    private void setupDisplayedAlphabet() {
        if (mLettersNumber > 0) {
            mDisplayedAlphabet.clear();

            if (mLettersNumber < mFullAlphabet.size()) {
                int step = (int) (Math.ceil((float) (mFullAlphabet.size()) / ((mLettersNumber - 1) / 2)));
                for (int i = 0; i < mFullAlphabet.size(); i += step) {
                    mDisplayedAlphabet.add(mFullAlphabet.get(i));
                    mDisplayedAlphabet.add(DOT_CHAR);
                }
            } else {
                mDisplayedAlphabet.addAll(mFullAlphabet);

            }
            mDisplayedAlphabet.add(NUMBER_CHAR);
        }
    }

    private void checkScroll(float y) {
        int spaceForDraw = getMeasuredHeight() - getPaddingTop() - getPaddingBottom();
        int letterHeight = spaceForDraw / mDisplayedAlphabet.size();
        float floatIndex = y / letterHeight;
        int index = (int) Math.floor(floatIndex);
        if (index >= 0 && index < mDisplayedAlphabet.size()) {
            String letter = mDisplayedAlphabet.get(index);
            if (mFullAlphabet.size() > mDisplayedAlphabet.size()) {
                if (letter.equals(DOT_CHAR)) {
                    int prevIndex = mFullAlphabet.indexOf(mDisplayedAlphabet.get(index - 1));
                    int nextIndex = mFullAlphabet.indexOf(mDisplayedAlphabet.get(index + 1));
                    float diff = nextIndex - prevIndex;
                    float progress = diff / (floatIndex + 1 - index);
                    int targetLetterIndex = Math.round(prevIndex + progress);
                    letter = mFullAlphabet.get(targetLetterIndex);
                }
            }
            if (mCurrentLetter == null || !mCurrentLetter.equals(letter)) {
                mCurrentLetter = letter;
                mAlphabetListener.onLetterScrolled(mCurrentLetter);
            }
        }
    }

    public void setAlphabetListener(AlphabetListener alphabetListener) {
        mAlphabetListener = alphabetListener;
    }

    public interface AlphabetListener {
        void onLetterScrolled(String letter);
    }
}
