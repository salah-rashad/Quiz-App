package com.bemo.quizapp;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.CountDownTimer;
import android.os.PersistableBundle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class QuizActivity extends AppCompatActivity {
    public static final String EXTRA_SCORE = "extraScore";
    public static final long COUNTDOWN_IN_MILLIS = 30000;

    private static final String KEY_SCORE = "keyScore";
    private static final String KEY_QUESTION_COUNT = "keyQuestionCount";
    private static final String KEY_MILLIS_LEFT = "keyMillisLeft";
    private static final String KEY_ANSWERED = "keyAnswered";
    private static final String KEY_QUESTION_LIST = "keyQuestionList";
    private static final String KEY_RADIO_GROUP = "keyRadioGroup";

    private TextView tvScore;
    private TextView tvQuestionCount;
    private TextView tvCategory;
    private TextView tvDifficulty;
    private TextView tvCountdown;
    private TextView tvQuestion;
    private RadioGroup radioGroup;
    private RadioButton rb1;
    private RadioButton rb2;
    private RadioButton rb3;
    private Button buttonConfirm;

    private int colorRed;
    private int colorGreen;

    private ColorStateList textColorDefaultRb;
    private ColorStateList textColorDefaultCd;

    private CountDownTimer countDownTimer;
    private long timeLeftInMillis;

    private ArrayList<Question> questionList;
    private int questionCounter;
    private int questionCountTotal;
    private Question currentQuestion;

    private int score;
    private boolean answered;
    private boolean rgIsLocked = false;

    private long backPressedTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        tvScore = findViewById(R.id.score_text_view);
        tvQuestionCount = findViewById(R.id.question_count_text_view);
        tvCategory = findViewById(R.id.category_text_view);
        tvDifficulty = findViewById(R.id.difficulty_text_view);
        tvCountdown = findViewById(R.id.countdown_text_view);
        tvQuestion = findViewById(R.id.question_text_view);
        radioGroup = findViewById(R.id.radio_group);
        rb1 = findViewById(R.id.radio_option1);
        rb2 = findViewById(R.id.radio_option2);
        rb3 = findViewById(R.id.radio_option3);
        buttonConfirm = findViewById(R.id.confirm_button);

        colorRed = getResources().getColor(R.color.colorRed);
        colorGreen = getResources().getColor(R.color.colorGreen);

        textColorDefaultRb = rb1.getTextColors();
        textColorDefaultCd = tvCountdown.getTextColors();

        Intent intent = getIntent();
        int categoryID = intent.getIntExtra(StartingScreenActivity.EXTRA_CATEGORY_ID, 0);
        String categoryName = intent.getStringExtra(StartingScreenActivity.EXTRA_CATEGORY_NAME);
        String difficulty = intent.getStringExtra(StartingScreenActivity.EXTRA_DIFFICULTY);

        tvCategory.setText("Category: " + categoryName);
        tvDifficulty.setText("Difficulty: " + difficulty);

        if (savedInstanceState == null) {
            QuizDbHelper quizDbHelper = QuizDbHelper.getInstance(this);
            questionList = quizDbHelper.getQuestions(categoryID, difficulty);
            questionCountTotal = questionList.size();
            Collections.shuffle(questionList);

            showNextQuestion();
        } else {
            questionList = savedInstanceState.getParcelableArrayList(KEY_QUESTION_LIST);
            questionCountTotal = questionList.size();
            questionCounter = savedInstanceState.getInt(KEY_QUESTION_COUNT);
            currentQuestion = questionList.get(questionCounter - 1);
            score = savedInstanceState.getInt(KEY_SCORE);
            timeLeftInMillis = savedInstanceState.getLong(KEY_MILLIS_LEFT);
            answered = savedInstanceState.getBoolean(KEY_ANSWERED);
            rgIsLocked = savedInstanceState.getBoolean(KEY_RADIO_GROUP);

            if (!answered) {
                startCountDown();
            } else {
                updateCountDownText();
                showSolution();
            }

            if (rgIsLocked) {
                rb1.setClickable(false);
                rb2.setClickable(false);
                rb3.setClickable(false);
            } else {
                rb1.setClickable(true);
                rb2.setClickable(true);
                rb3.setClickable(true);
            }
        }

        buttonConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!answered) {
                    if (rb1.isChecked() || rb2.isChecked() || rb3.isChecked()) {
                        checkAnswer();
                    } else {
                        Toast.makeText(QuizActivity.this, "Please select an answer", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    showNextQuestion();
                }
            }
        });
    }

    private void showNextQuestion() {
        rb1.setTextColor(textColorDefaultRb);
        rb2.setTextColor(textColorDefaultRb);
        rb3.setTextColor(textColorDefaultRb);
        radioGroup.clearCheck();

        rb1.setClickable(true);
        rb2.setClickable(true);
        rb3.setClickable(true);
        rgIsLocked = false;

        if (questionCounter < questionCountTotal) {
            currentQuestion = questionList.get(questionCounter);

            tvQuestion.setText(currentQuestion.getQuestion());
            rb1.setText(currentQuestion.getOption1());
            rb2.setText(currentQuestion.getOption2());
            rb3.setText(currentQuestion.getOption3());

            questionCounter++;
            tvQuestionCount.setText("Question: " + questionCounter + "/" + questionCountTotal);
            answered = false;
            buttonConfirm.setText("Confirm");

            timeLeftInMillis = COUNTDOWN_IN_MILLIS;
            startCountDown();
        } else {
            finishQuiz();
        }
    }

    private void startCountDown() {
        countDownTimer = new CountDownTimer(timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                updateCountDownText();
            }

            @Override
            public void onFinish() {
                timeLeftInMillis = 0;
                updateCountDownText();
                checkAnswer();
            }
        }.start();
    }

    private void updateCountDownText() {
        int minutes = (int) (timeLeftInMillis / 1000) / 60;
        int seconds = (int) (timeLeftInMillis / 1000) % 60;

        String timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);

        tvCountdown.setText(timeFormatted);

        if (timeLeftInMillis < 10000) {
            tvCountdown.setTextColor(colorRed);
        }
    }

    private void checkAnswer() {
        answered = true;

        countDownTimer.cancel();

        RadioButton rbSelected = findViewById(radioGroup.getCheckedRadioButtonId());
        int answerNr = radioGroup.indexOfChild(rbSelected) + 1;

        if (answerNr == currentQuestion.getAnswerNr()) {
            score++;
            tvScore.setText("Score: " + score);
        }

        showSolution();
    }

    private void showSolution() {
        rb1.setTextColor(colorRed);
        rb2.setTextColor(colorRed);
        rb3.setTextColor(colorRed);

        rb1.setClickable(false);
        rb2.setClickable(false);
        rb3.setClickable(false);
        rgIsLocked = true;

        switch (currentQuestion.getAnswerNr()) {
            case 1:
                rb1.setTextColor(colorGreen);
                tvQuestion.setText("Answer 1 is correct");
                break;
            case 2:
                rb2.setTextColor(colorGreen);
                tvQuestion.setText("Answer 2 is correct");
                break;
            case 3:
                rb3.setTextColor(colorGreen);
                tvQuestion.setText("Answer 3 is correct");
                break;
        }

        if (questionCounter < questionCountTotal) {
            buttonConfirm.setText("Next");
        } else {
            buttonConfirm.setText("Finish");
        }
    }

    private void finishQuiz() {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(EXTRA_SCORE, score);
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (backPressedTime + 2000 > System.currentTimeMillis()) {
            finishQuiz();
        } else {
            Toast.makeText(this, "Press back again to finish", Toast.LENGTH_SHORT).show();
        }

        backPressedTime = System.currentTimeMillis();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_SCORE, score);
        outState.putInt(KEY_QUESTION_COUNT, questionCounter);
        outState.putLong(KEY_MILLIS_LEFT, timeLeftInMillis);
        outState.putBoolean(KEY_ANSWERED, answered);
        outState.putParcelableArrayList(KEY_QUESTION_LIST, questionList);
        outState.putBoolean(KEY_RADIO_GROUP, rgIsLocked);

    }
}
