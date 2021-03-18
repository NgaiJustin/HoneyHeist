using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class PlayerRotator : MonoBehaviour
{
    public bool rotating = false;
    public bool clampDuringRotation = true;
    const int ROTATION_DEGREES = 60;
    const float DURATION_OF_ROTATION = .5f;
    Coroutine currentCoroutine;

    public float DELAY = .5f;

    public Player player;

    public float originalGravityScale;

    public void Rotate(bool left)
    {
        float rotation = ROTATION_DEGREES;
        if (left) rotation *= -1;

        if (!rotating)
        {
            if (currentCoroutine != null)
            {
                StopCoroutine(currentCoroutine);
            }
            currentCoroutine = StartCoroutine(RotationLerp(this.transform, rotation, DURATION_OF_ROTATION));
        }
    }

    public void StopClamp()
    {
        if (currentCoroutine != null)
        {
            StopCoroutine(currentCoroutine);
        }
        player.UnClampPlayer();
        player.UnFreezePlayer();
    }

    public void StopRotation()
    {
        if (currentCoroutine != null)
        {
            StopCoroutine(currentCoroutine);
        }
        player.rb.gravityScale = originalGravityScale;
        rotating = false;
    }   

    private IEnumerator RotationLerp(Transform transform, float rotation, float duration)
    {
        rotating = true;
        if (clampDuringRotation)
        {
            player.ClampPlayer();
            player.FreezePlayer();
        }
        Quaternion initialRotation = transform.rotation;
        Quaternion rotationTarget = Quaternion.Euler(0,0,transform.eulerAngles.z + rotation);
        float t = 0;
        while(t < duration)
        {
            t += Time.deltaTime;
            transform.rotation = Quaternion.Slerp(initialRotation, rotationTarget, t/duration);
            yield return null;
        }
        rotating = false;
        yield return new WaitForSeconds(DELAY);
        player.UnClampPlayer();
        player.UnFreezePlayer();
        yield return null;
    }
}
