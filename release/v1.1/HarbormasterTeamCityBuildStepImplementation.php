<?php

final class HarbormasterTeamCityBuildStepImplementation
  extends HarbormasterBuildStepImplementation {

  public function getName() {
    return pht('Build with TeamCity');
  }

  public function getGenericDescription() {
    return pht('Trigger TeamCity Builds with Harbormaster');
  }

  public function getBuildStepGroupKey() {
    return HarbormasterExternalBuildStepGroup::GROUPKEY;
  }

  public function getDescription() {
    $domain = null;
    $uri = $this->getSetting('uri');
    if ($uri) {
      $domain = id(new PhutilURI($uri))->getDomain();
    }

    $method = $this->formatSettingForDescription('method', 'POST');
    $domain = $this->formatValueForDescription($domain);

    if ($this->getSetting('credential')) {
      return pht(
        'Make an authenticated HTTP %s request to %s.',
        $method,
        $domain);
    } else {
      return pht(
        'Make an HTTP %s request to %s.',
        $method,
        $domain);
    }
  }

  public function execute(
    HarbormasterBuild $build,
    HarbormasterBuildTarget $build_target) {

    $viewer = PhabricatorUser::getOmnipotentUser();
    $settings = $this->getSettings();
    $variables = $build_target->getVariables();

    $uri = $settings['uri'] . '/httpAuth/app/rest/buildQueue';

    $method = 'POST';
    $contentType = 'application/xml';

    $xmlBuilder = new TeamCityXmlBuildBuilder();
    $payload = $xmlBuilder
        ->addBuildId($settings['buildId'])
        ->addBranchName(implode(array("Phab-D", $variables['buildable.revision'])))
        ->addDiffId($variables['buildable.diff'])
        ->addHarbormasterPHID($variables['target.phid'])
        ->addRevisionId($variables['buildable.revision'])
        ->build();

    $future = id(new HTTPFuture($uri, $payload))
      ->setMethod($method)
      ->addHeader('Content-Type', $contentType)
      ->setTimeout(60);

    $credential_phid = $this->getSetting('credential');
    if ($credential_phid) {
      $key = PassphrasePasswordKey::loadFromPHID(
        $credential_phid,
        $viewer);
      $future->setHTTPBasicAuthCredentials(
        $key->getUsernameEnvelope()->openEnvelope(),
        $key->getPasswordEnvelope());
    }

    $this->resolveFutures(
      $build,
      $build_target,
      array($future));

    list($status, $body, $headers) = $future->resolve();

    $this->logHTTPResponse($build, $build_target, $future, pht('TeamCity'));

    $build_target
      ->newLog($label, 'http.req.body')
      ->append($future->getData());

    if ($status->isError()) {
      throw new HarbormasterBuildFailureException();
    }
  }

  public function getFieldSpecifications() {
    return array(
      'uri' => array(
        'name' => pht('URI'),
        'type' => 'text',
        'required' => true,
      ),
      'buildId' => array(
        'name' => pht('TeamCity Build Configuration ID'),
        'type' => 'text',
        'required' => true,
      ),
      'credential' => array(
          'name' => pht('TeamCity Credentials'),
          'type' => 'credential',
          'required' => true,
          'credential.type'
          => PassphrasePasswordCredentialType::CREDENTIAL_TYPE,
          'credential.provides'
          => PassphrasePasswordCredentialType::PROVIDES_TYPE,
      ),
    );
  }

  public function supportsWaitForMessage() {
    return true;
  }

}