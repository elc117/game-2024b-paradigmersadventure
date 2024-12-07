package com.paradigmas.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.paradigmas.game.Main;
import com.paradigmas.game.entities.Enemy;
import com.paradigmas.game.entities.Paradigmer;
import com.paradigmas.game.questions.Question;
import com.paradigmas.game.questions.Quiz;
import com.paradigmas.game.ui.Button;
import com.paradigmas.game.ui.ButtonAction;
import com.paradigmas.game.ui.QuizGenerator;
import com.paradigmas.game.utils.BattleStatus;
import com.paradigmas.game.utils.LoadAssets;
import com.paradigmas.game.utils.ScreenType;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;

import static com.paradigmas.game.utils.FontType.*;
import static com.paradigmas.game.utils.ScreenType.*;
import static com.paradigmas.game.utils.BattleStatus.*;

@Getter
public class BattleScreen extends SuperScreen {
    private final Quiz quiz;
    private final Enemy enemy;
    private final Paradigmer paradigmer;
    private final Sprite uiTextSprite;
    private final Sprite uiLifeBarSprite;
    private final Sprite uiQuestionSprite;
    private BattleStatus status;
    private Question actualQuestion;
    private boolean isCorrect;
    private float timePassed;
    private float timeText;
    private int currentCharIndex;

    public BattleScreen(Main game, String backgroundTexturePath, String backgroundMusicPath, Quiz quiz, Enemy enemy, Paradigmer paradigmer) {
        super(game, backgroundTexturePath, backgroundMusicPath);
        uiTextSprite = LoadAssets.loadSprite("sprites/uiMenu.png", 17, 3);
        uiLifeBarSprite = LoadAssets.loadSprite("sprites/lifeBar.png", 5, 2);
        uiQuestionSprite = LoadAssets.loadSprite("sprites/questionSelection.png", 8, 4);
        this.quiz = quiz;
        this.enemy = enemy;
        this.paradigmer = paradigmer;
        currentCharIndex = 0;

        status = WAITING_ANSWER;
        actualQuestion = quiz.getQuestion();

        String text = "Pause";
        ButtonAction action = () -> super.game.getScreenManager().showScreen(PAUSE_SCREEN);
        Button button = new Button(
            super.game,
            text,
            super.getWorldWidth() - 1,
            super.getWorldHeight() - 1,
            1.3f,
            1f,
            action
        );

        super.addButton(button);

        initializeButtons();
    }

    @Override
    public void draw(float delta) {
        ScreenUtils.clear(Color.BLACK);
        super.game.getViewport().apply();
        super.game.getBatch().setProjectionMatrix(
            super.game.getViewport().getCamera().combined
        );

        super.game.getBatch().begin();

        super.game.getBatch().draw(
            backgroundTexture,
            0,
            0,
            worldWidth,
            worldHeight
        );

        //sprite inimigo
        enemy.getSprite().setPosition(super.worldWidth/2 + 0.5f, super.worldHeight/2 - 0.9f);
        enemy.getSprite().draw(game.getBatch());

        //sprite paradigmer
        paradigmer.getSprite().setPosition(super.worldWidth/2 - 5.5f, super.worldHeight/2 - 3.5f);
        paradigmer.getSprite().draw(game.getBatch());

        //caixa onde o texto fica
        uiTextSprite.setPosition(-0.4f, -0.4f);
        uiTextSprite.draw(game.getBatch());

        //barra de vida do inimigo
        uiLifeBarSprite.setPosition(0, super.worldHeight - 2);
        uiLifeBarSprite.draw(game.getBatch());
        super.game.getFontHashMap().get(TEXT_BATTLE).draw(
            super.game.getBatch(),
            enemy.getName(),
            0.5f,
            super.worldHeight - 0.5f
        );

        //Barra de vida do jogador
        uiLifeBarSprite.setPosition(super.worldWidth - 4.6f, 2.9f);
        uiLifeBarSprite.draw(game.getBatch());
        super.game.getFontHashMap().get(TEXT_BATTLE).draw(
            super.game.getBatch(),
            paradigmer.getName(),
            super.worldWidth - 4.1f,
            4.4f
        );
        super.game.getFontHashMap().get(TEXT_BATTLE).draw(
            super.game.getBatch(),
            String.format("%d/100",paradigmer.getLife()),
            super.worldWidth - 2.5f,
            3.57f
        );

        if (status == WAITING_ANSWER) {
            //caixa de respostas
            uiQuestionSprite.setPosition(super.worldWidth - 7.69f, -0.6f);
            uiQuestionSprite.draw(game.getBatch());
        }

        //talvez tenha ficado meio confuso
        String textToDraw = (status == WAITING_ANSWER) ?
            actualQuestion.getQuestion()
            :
            (isCorrect) ?
                String.format("Resposta correta! %d acertos consecutivos!", paradigmer.getConsecutiveHits())
                :
                "Resposta errada!";
        textToDraw = breakText(textToDraw, delta);
        float x = 0.5f;
        float y = 2f;
        int alignment = Align.left;
        super.game.getFontHashMap().get(TEXT_QUESTION).draw(
            super.game.getBatch(),
            textToDraw,
            x,
            y,
            7.5f,
            alignment,
            true
        );

        super.game.getBatch().end();

        super.stage.act(
            Math.min(
                Gdx.graphics.getDeltaTime(), 1 / 30f
            )
        );
        super.stage.draw();
    }

    @Override
    public void logic(float delta) {
        switch (status) {
            case ANSWERED:
                currentCharIndex = 0;
                super.removeButtons(1); //Remove todos os botões de responder questão
                if (isCorrect) {
                    paradigmer.hit();
                    enemy.causeDamage(paradigmer.getConsecutiveHits());
                } else {
                    paradigmer.miss();
                    paradigmer.causeDamage();
                }

                timePassed += delta;

                status = WAITING_NEXT_QUESTION;
                break;
            case WAITING_NEXT_QUESTION:
                timePassed += delta;

                if (timePassed >= 3f || Gdx.input.isTouched()) {
                    actualQuestion = quiz.getQuestion();
                    initializeButtons();
                    currentCharIndex = 0;
                    status = WAITING_ANSWER;
                }
                break;
        }

        if (paradigmer.getLife() == 0) {
            setEndGameScreen(true);
        } else if (enemy.getLife() == 0) {
            setEndGameScreen(false);
        }
    }

    private void setEndGameScreen(boolean result) {
        ScreenManager screenManager = super.game.getScreenManager();
        HashMap<ScreenType, SuperScreen> screens = screenManager.getScreens();
        screens.remove(END_SCREEN);

        String backgroundTexturePath = "images/battleBackground.jpg";
        String backgroundMusicPath = "sounds/backgroundMusic.mp3";
       EndGameScreen endGameScreen = new EndGameScreen(super.game, backgroundTexturePath, backgroundMusicPath, result);
        screens.put(END_SCREEN, endGameScreen);
        screenManager.showScreen(END_SCREEN);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(super.stage);
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }

    @Override
    public void dispose() {
        super.dispose();
        uiTextSprite.getTexture().dispose();
        uiLifeBarSprite.getTexture().dispose();
    }

    private String breakText(String text, float delta) {
        timeText += delta;

        float timePerChar = 0.03f;
        if (timeText >= timePerChar) {
            timeText = 0f;
            if (currentCharIndex < text.length()) {
                currentCharIndex++;
            }
        }

        return text.substring(0, currentCharIndex);
    }

    private void questionAnswered(int option) {
        isCorrect = actualQuestion.answerQuestion(option);
        status = ANSWERED;
        timePassed = 0f;
    }

    private void initializeButtons() {
        float buttonDistanceY = 0;
        int cont = 0;
        for (int i = 0; i < 2; i++) {
            float buttonDistanceX = 0;

            for (int j = 0; j < 2; j++) {
                int option = cont + 1;
                ButtonAction action = () -> questionAnswered(option);

                Button button = new Button(
                    super.game,
                    actualQuestion.getAnswer(cont),
                    super.worldWidth - 7f + buttonDistanceX,
                    1.7f - buttonDistanceY,
                    3f,
                    1f,
                    action
                );

                super.addButton(button);
                buttonDistanceX += 3.7f;
                cont++;
            }
            buttonDistanceY += 1.5f;
        }
    }
}
