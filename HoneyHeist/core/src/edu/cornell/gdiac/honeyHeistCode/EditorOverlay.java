package edu.cornell.gdiac.honeyHeistCode;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;

public class EditorOverlay extends ApplicationAdapter implements Screen {
    private Stage stage;
    private Table table;
    private Skin skin;

    public EditorOverlay() {
        this.create();
        this.render(0);
    }

    @Override
    public void create(){
        skin = new Skin(Gdx.files.internal("shadeui/uiskin.json"));
        stage = new Stage();

        table = new Table();
        table.setFillParent(true);

        table.setDebug(true); // This is optional, but enables debug lines for tables.

        Label nameLabel = new Label("Name:", skin);
        TextField nameText = new TextField("", skin);
        Label addressLabel = new Label("Address:", skin);
        TextField addressText = new TextField("", skin);

        table.add(nameLabel).expand();
        table.add(nameText).expand();

        stage.addActor(table);
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {

    }

//    @Override
//    public void render(float delta) {
//        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
//        stage.act(Gdx.graphics.getDeltaTime());
//        stage.draw();
//    }

    @Override
    public void hide() {

    }

    @Override
    public void dispose() {
        stage.dispose();
    }
}
