<?php
include 'arta/ArtaCli.php';


class Main extends ArtaCli {

    public function startApp() {
        chdir(dirname(__FILE__));
        $this->build(dirname(__FILE__) . '/config.ini');
    }
}

$main = new Main();
$main->startApp();
